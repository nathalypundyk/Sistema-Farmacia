package sistema_farmacia.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sistema_farmacia.demo.model.Medicamento;
import sistema_farmacia.demo.repository.MedicamentoRepository;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WhatsappService {
    @Autowired
    private MedicamentoRepository medicamentoRepo;

    // Estos valores se configuran en application.properties
    @Value("${whatsapp.api.url}")
    private String whatsappApiUrl;

    @Value("${whatsapp.encargada.numero}")
    private String numeroEncargadaDeposito;

    /**
     * Consulta la BD, arma el mensaje según el estado del stock/lotes
     * y lo envía al número de la encargada del depósito vía el gateway WhatsApp.
     */
    public void enviarAlertaDiaria() {
        List<Medicamento> todos = medicamentoRepo.findAll();

        // Filtrar medicamentos con stock crítico
        List<Medicamento> conStockCritico = todos.stream()
                .filter(Medicamento::isStockBajo)
                .collect(Collectors.toList());

        // Filtrar medicamentos con lotes próximos a vencer (< 30 días)
        List<Medicamento> conLotesAVencer = todos.stream()
                .filter(Medicamento::isProximoAVencer)
                .collect(Collectors.toList());

        String mensaje = construirMensaje(conStockCritico, conLotesAVencer);

        enviarMensaje(numeroEncargadaDeposito, mensaje);
    }

    /**
     * Construye el texto del mensaje según los 4 casos posibles.
     */
    private String construirMensaje(List<Medicamento> criticos, List<Medicamento> aVencer) {

        boolean hayCriticos = !criticos.isEmpty();
        boolean hayAVencer  = !aVencer.isEmpty();

        if (!hayCriticos && !hayAVencer) {
            // Caso 1: Todo bien
            return "✅ *Alerta diaria - Farmacia HGS*\n\n"
                 + "No hay medicamentos con stock crítico ni lotes próximos a vencer.";
        }

        if (hayCriticos && !hayAVencer) {
            // Caso 2: Solo stock crítico
            return "⚠️ *Alerta diaria - Farmacia HGS*\n\n"
                 + "Hay *" + criticos.size() + "* medicamento(s) con stock crítico, "
                 + "por favor revise el sistema para más información.\n\n"
                 + "No hay lotes próximos a vencer.";
        }

        if (!hayCriticos && hayAVencer) {
            // Caso 3: Solo lotes a punto de vencer
            return "⚠️ *Alerta diaria - Farmacia HGS*\n\n"
                 + "No hay medicamentos con stock crítico.\n\n"
                 + "Hay *" + aVencer.size() + "* medicamento(s) con lotes próximos a vencer "
                 + "(menos de 30 días), por favor revise el sistema para más información.";
        }

        // Caso 4: Ambos
        return "🚨 *Alerta diaria - Farmacia HGS*\n\n"
             + "Hay *" + criticos.size() + "* medicamento(s) con stock crítico, "
             + "por favor revise el sistema para más información.\n\n"
             + "Hay *" + aVencer.size() + "* medicamento(s) con lotes próximos a vencer "
             + "(menos de 30 días), por favor revise el sistema para más información.";
    }

    /**
     * Envía un mensaje libre a la encargada del depósito.
     * Lo usa el módulo de cámara de frío (alertas de temperatura, puerta e intrusos).
     */
    public void enviarMensajeEncargada(String mensaje) {
        enviarMensaje(numeroEncargadaDeposito, mensaje);
    }

    /**
     * Llama al endpoint /send del contenedor WhatsApp.
     */
    private void enviarMensaje(String numero, String mensaje) {
        try {
            String mensajeCodificado = URLEncoder.encode(mensaje, StandardCharsets.UTF_8);
            String url = whatsappApiUrl + "/send?number=" + numero + "&message=" + mensajeCodificado;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✅ Alerta WhatsApp enviada correctamente a " + numero);
            } else {
                System.err.println("❌ Error al enviar WhatsApp. Código: " + response.statusCode()
                        + " | Respuesta: " + response.body());
            }

        } catch (Exception e) {
            System.err.println("❌ Excepción al enviar mensaje WhatsApp: " + e.getMessage());
        }
    }    
}
