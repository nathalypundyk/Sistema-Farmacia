package sistema_farmacia.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sistema_farmacia.demo.model.*;
import sistema_farmacia.demo.repository.*;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lógica del módulo "Cadena de frío":
 *  - guarda las lecturas de temperatura y los accesos que envía el dispositivo,
 *  - decide si corresponde alertar (temperatura fuera de rango, puerta abierta demasiado tiempo, intruso),
 *  - administra el permiso individual y la foto de rostro de cada usuario.
 */
@Service
public class CamaraFrioService {

    public static final String ALERTA_TEMPERATURA = "CAMARA_FRIO_TEMPERATURA";
    public static final String ALERTA_PUERTA      = "CAMARA_FRIO_PUERTA";
    public static final String ALERTA_INTRUSO     = "CAMARA_FRIO_INTRUSO";

    private static final long FOTO_MAX_BYTES = 2 * 1024 * 1024; // 2 MB
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final LecturaTemperaturaRepository lecturaRepo;
    private final AccesoCamaraFrioRepository accesoRepo;
    private final AutorizacionCamaraFrioRepository autorizacionRepo;
    private final UsuarioRepository usuarioRepo;
    private final AlertaRepository alertaRepo;
    private final WhatsappService whatsappService;

    /** Umbrales. Por defecto coinciden con el código Arduino del docente (umbralMin = 18, umbralMax = 22). */
    @Value("${camarafrio.temp.min:18}")
    private double tempMin;

    @Value("${camarafrio.temp.max:22}")
    private double tempMax;

    /** Minutos mínimos entre dos alertas del mismo tipo, para no saturar el WhatsApp. */
    @Value("${camarafrio.alerta.cooldown-minutos:10}")
    private long cooldownMinutos;

    /** Si no llega una lectura en este tiempo, el dispositivo se muestra como desconectado. */
    @Value("${camarafrio.dispositivo.timeout-segundos:60}")
    private long timeoutSegundos;

    private final Map<String, LocalDateTime> ultimaAlertaPorTipo = new ConcurrentHashMap<>();

    public CamaraFrioService(LecturaTemperaturaRepository lecturaRepo,
                             AccesoCamaraFrioRepository accesoRepo,
                             AutorizacionCamaraFrioRepository autorizacionRepo,
                             UsuarioRepository usuarioRepo,
                             AlertaRepository alertaRepo,
                             WhatsappService whatsappService) {
        this.lecturaRepo = lecturaRepo;
        this.accesoRepo = accesoRepo;
        this.autorizacionRepo = autorizacionRepo;
        this.usuarioRepo = usuarioRepo;
        this.alertaRepo = alertaRepo;
        this.whatsappService = whatsappService;
    }

    // ───────────────────────── Lecturas ─────────────────────────

    @Transactional
    public LecturaTemperatura registrarLectura(Double temperatura, String puerta, String tiempo, String estado) {
        if (temperatura == null) {
            throw new IllegalArgumentException("La temperatura es obligatoria");
        }
        // DallasTemperature devuelve -127 cuando el sensor está desconectado.
        if (temperatura <= -100) {
            throw new IllegalArgumentException("Lectura inválida: sensor desconectado (-127 °C)");
        }

        boolean abierta = "ABIERTA".equalsIgnoreCase(puerta);
        boolean fuera = temperatura < tempMin || temperatura > tempMax;

        LecturaTemperatura lectura = lecturaRepo.save(LecturaTemperatura.builder()
                .temperatura(temperatura)
                .puertaAbierta(abierta)
                .tiempoPuerta(tiempo)
                .estadoDispositivo(estado)
                .fueraDeRango(fuera)
                .build());

        if (fuera) {
            String sentido = temperatura > tempMax ? "por ENCIMA del máximo" : "por DEBAJO del mínimo";
            alertar(ALERTA_TEMPERATURA,
                    "🌡️ *Cámara de frío - Farmacia HGS*\n\n"
                  + "Temperatura " + String.format(Locale.US, "%.2f", temperatura) + " °C, " + sentido
                  + " (rango permitido " + tempMin + " a " + tempMax + " °C).\n"
                  + "Hora: " + lectura.getFechaHora().format(HORA));
        }
        if (estado != null && estado.toLowerCase().contains("tiempo max")) {
            alertar(ALERTA_PUERTA,
                    "🚪 *Cámara de frío - Farmacia HGS*\n\n"
                  + "La puerta lleva abierta " + tiempo + " (supera el máximo permitido).\n"
                  + "Hora: " + lectura.getFechaHora().format(HORA));
        }
        return lectura;
    }

    public Optional<LecturaTemperatura> ultimaLectura() {
        return lecturaRepo.findFirstByOrderByFechaHoraDesc();
    }

    public List<LecturaTemperatura> lecturasDesde(LocalDateTime desde) {
        return lecturaRepo.findByFechaHoraAfterOrderByFechaHoraAsc(desde);
    }

    public boolean dispositivoConectado() {
        return ultimaLectura()
                .map(l -> Duration.between(l.getFechaHora(), LocalDateTime.now()).getSeconds() <= timeoutSegundos)
                .orElse(false);
    }

    // ───────────────────────── Accesos ─────────────────────────

    @Transactional
    public AccesoCamaraFrio registrarAcceso(Long usuarioId, String nombreDetectado, Boolean puertaAbierta, Double distancia) {
        Usuario usuario = usuarioId != null ? usuarioRepo.findById(usuarioId).orElse(null) : null;

        // El servidor decide el resultado con el permiso vigente en la base de datos,
        // así un permiso revocado se respeta aunque el script no haya recargado la lista.
        String resultado;
        if (usuario == null) {
            resultado = AccesoCamaraFrio.DENEGADO_DESCONOCIDO;
        } else {
            boolean autorizado = Boolean.TRUE.equals(usuario.getActivo())
                    && autorizacionRepo.findByUsuario(usuario)
                                       .map(a -> Boolean.TRUE.equals(a.getAutorizado()))
                                       .orElse(false);
            resultado = autorizado ? AccesoCamaraFrio.AUTORIZADO : AccesoCamaraFrio.DENEGADO_SIN_PERMISO;
        }

        AccesoCamaraFrio acceso = accesoRepo.save(AccesoCamaraFrio.builder()
                .usuario(usuario)
                .nombreDetectado(usuario != null ? nombreCompleto(usuario) : (nombreDetectado != null ? nombreDetectado : "Desconocido"))
                .resultado(resultado)
                .puertaAbierta(Boolean.TRUE.equals(puertaAbierta))
                .distancia(distancia)
                .build());

        if (!AccesoCamaraFrio.AUTORIZADO.equals(resultado) && Boolean.TRUE.equals(puertaAbierta)) {
            String quien = usuario != null
                    ? nombreCompleto(usuario) + " (usuario SIN permiso de acceso)"
                    : "una persona NO registrada";
            alertar(ALERTA_INTRUSO,
                    "🚨 *Cámara de frío - Farmacia HGS*\n\n"
                  + "Se detectó a " + quien + " con la puerta de la cámara abierta.\n"
                  + "Hora: " + acceso.getFechaHora().format(HORA));
        }
        return acceso;
    }

    public List<AccesoCamaraFrio> ultimosAccesos() {
        return accesoRepo.findTop50ByOrderByFechaHoraDesc();
    }

    // ───────────────────── Autorizaciones y fotos ─────────────────────

    /** Lista que descarga el script de reconocimiento: solo usuarios activos con foto cargada. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> rostrosParaDispositivo() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (AutorizacionCamaraFrio a : autorizacionRepo.findAll()) {
            Usuario u = a.getUsuario();
            if (!a.tieneFoto() || u == null || !Boolean.TRUE.equals(u.getActivo())) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("usuarioId", u.getId());
            m.put("nombre", nombreCompleto(u));
            m.put("autorizado", Boolean.TRUE.equals(a.getAutorizado()));
            m.put("actualizado", a.getFechaActualizacion() != null ? a.getFechaActualizacion().toString() : null);
            lista.add(m);
        }
        return lista;
    }

    @Transactional(readOnly = true)
    public Optional<AutorizacionCamaraFrio> autorizacionDe(Long usuarioId) {
        return autorizacionRepo.findByUsuarioId(usuarioId);
    }

    @Transactional(readOnly = true)
    public Map<Long, AutorizacionCamaraFrio> autorizacionesPorUsuario() {
        Map<Long, AutorizacionCamaraFrio> mapa = new HashMap<>();
        for (AutorizacionCamaraFrio a : autorizacionRepo.findAll()) {
            mapa.put(a.getUsuario().getId(), a);
        }
        return mapa;
    }

    @Transactional
    public void guardarAutorizacion(Long usuarioId, boolean autorizado, MultipartFile foto, String admin) throws IOException {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario inexistente"));

        AutorizacionCamaraFrio a = autorizacionRepo.findByUsuario(usuario)
                .orElseGet(() -> AutorizacionCamaraFrio.builder().usuario(usuario).build());

        if (foto != null && !foto.isEmpty()) {
            String tipo = foto.getContentType();
            if (tipo == null || !(tipo.equals("image/jpeg") || tipo.equals("image/png"))) {
                throw new IllegalArgumentException("La foto debe ser JPG o PNG");
            }
            if (foto.getSize() > FOTO_MAX_BYTES) {
                throw new IllegalArgumentException("La foto no puede superar 2 MB");
            }
            a.setFotoRostro(foto.getBytes());
            a.setFotoContentType(tipo);
        }
        if (autorizado && !a.tieneFoto()) {
            throw new IllegalArgumentException("Para autorizar el acceso primero hay que cargar la foto del rostro");
        }

        a.setAutorizado(autorizado);
        a.setFechaActualizacion(LocalDateTime.now());
        a.setActualizadoPor(admin);
        autorizacionRepo.save(a);
    }

    // ───────────────────────── Alertas ─────────────────────────

    /**
     * Registra la alerta en la tabla "alertas" (la misma del resto del sistema) y la envía por
     * WhatsApp a la encargada del depósito. Respeta un tiempo mínimo entre alertas del mismo tipo.
     */
    private void alertar(String tipo, String mensaje) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime ultima = ultimaAlertaPorTipo.get(tipo);
        if (ultima != null && Duration.between(ultima, ahora).toMinutes() < cooldownMinutos) {
            return;
        }
        ultimaAlertaPorTipo.put(tipo, ahora);

        alertaRepo.save(Alerta.builder()
                .tipo(tipo)
                .mensaje(mensaje.replace("*", ""))
                .build());

        // El envío es asíncrono para no demorar la respuesta al dispositivo.
        CompletableFuture.runAsync(() -> whatsappService.enviarMensajeEncargada(mensaje));
    }

    // ───────────────────────── Utilidades ─────────────────────────

    public double getTempMin() { return tempMin; }
    public double getTempMax() { return tempMax; }

    public static String nombreCompleto(Usuario u) {
        String n = ((u.getNombre() != null ? u.getNombre() : "") + " "
                  + (u.getApellido() != null ? u.getApellido() : "")).trim();
        return n.isEmpty() ? u.getUsername() : n;
    }
}
