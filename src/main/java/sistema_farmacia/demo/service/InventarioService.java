package sistema_farmacia.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sistema_farmacia.demo.model.*;
import sistema_farmacia.demo.repository.*;


import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InventarioService {

    @Autowired private MedicamentoRepository medicamentoRepo;
    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private LoteRepository loteRepo;
    @Autowired private AuditoriaAjusteRepository auditoriaRepo;
    @Autowired private AlertaRepository alertaRepo;

    @Transactional
    public void registrarSalida(Long medicamentoId, Integer cantidad, String pinProvided) throws Exception {
        Medicamento med = medicamentoRepo.findById(medicamentoId)
                .orElseThrow(() -> new Exception("Medicamento no encontrado"));

        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario currentUser = usuarioRepo.findByUsername(currentUsername)
                .orElseThrow(() -> new Exception("Usuario no autenticado"));

        // 1. Regla de Integridad de Blister
        if (med.getUnidadesPorBlister() != null && cantidad % med.getUnidadesPorBlister() != 0) {
            throw new Exception("No se puede fraccionar el blister. Debe ser múltiplo de " + med.getUnidadesPorBlister());
        }

        // 2. Seguridad PIN Personal para drogas críticas
        boolean pinValido = currentUser.getPin() != null && currentUser.getPin().equals(pinProvided);
        if (med.getEsPrioridad()) {
            if (!pinValido) {
                registrarAuditoria(med, currentUser.getUsername(), "FALLO_PIN_CRITICO", cantidad, null, pinProvided, false);
                throw new Exception("PIN de autorización inválido para droga crítica");
            }
        }

        // 3. Lógica FEFO (First Expired, First Out)
        List<Lote> lotesDisponibles = med.getLotes().stream()
                .filter(l -> l.getCantidadDisponible() > 0)
                .sorted(Comparator.comparing(Lote::getFechaVencimiento))
                .collect(Collectors.toList());

        int stockTotal = med.getStockTotal();
        if (stockTotal < cantidad) {
            throw new Exception("Stock insuficiente");
        }

        int pendiente = cantidad;
        int stockAnterior = stockTotal;

        for (Lote lote : lotesDisponibles) {
            if (pendiente <= 0) break;

            int disponible = lote.getCantidadDisponible();
            if (disponible >= pendiente) {
                lote.setCantidadDisponible(disponible - pendiente);
                pendiente = 0;
            } else {
                lote.setCantidadDisponible(0);
                pendiente -= disponible;
            }
            loteRepo.save(lote);
        }

        registrarAuditoria(med, currentUser.getUsername(), "SALIDA", stockAnterior, stockAnterior - cantidad, pinProvided, pinValido);
        verificarAlertas(med);
    }

    @Transactional
    public void registrarDevolucion(Long loteId, Integer cantidad, String motivo) throws Exception {
        Lote lote = loteRepo.findById(loteId)
                .orElseThrow(() -> new Exception("Lote no encontrado"));
        
        Medicamento med = lote.getMedicamento();
        int stockAnterior = med.getStockTotal();
        
        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        
        lote.setCantidadDisponible(lote.getCantidadDisponible() + cantidad);
        loteRepo.save(lote);
        
        registrarAuditoria(med, currentUsername, "DEVOLUCION - " + motivo, stockAnterior, stockAnterior + cantidad, null, true);
        verificarAlertas(med);
    }

    @Transactional
    public void ajustarStock(Long medicamentoId, Integer nuevoStock, String pinProvided) throws Exception {
        Medicamento med = medicamentoRepo.findById(medicamentoId)
                .orElseThrow(() -> new Exception("Medicamento no encontrado"));

        String currentUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        Usuario currentUser = usuarioRepo.findByUsername(currentUsername)
                .orElseThrow(() -> new Exception("Usuario no autenticado"));

        boolean pinValido = currentUser.getPin() != null && currentUser.getPin().equals(pinProvided);
        if (!pinValido) {
            registrarAuditoria(med, currentUser.getUsername(), "AJUSTE_FALLIDO_PIN", med.getStockTotal(), nuevoStock, pinProvided, false);
            throw new Exception("PIN personal requerido para ajuste manual");
        }

        int stockAnterior = med.getStockTotal();
        // Lógica de ajuste simplificada...
        
        registrarAuditoria(med, currentUser.getUsername(), "AJUSTE_MANUAL", stockAnterior, nuevoStock, pinProvided, true);
        verificarAlertas(med);
    }

    private void registrarAuditoria(Medicamento med, String username, String accion, Integer anterior, Integer nuevo, String pin, Boolean verificado) {
        AuditoriaAjuste auditoria = AuditoriaAjuste.builder()
                .usuario(username)
                .accion(accion)
                .medicamentoNombre(med.getNombre())
                .cantidadAnterior(anterior)
                .cantidadNueva(nuevo)
                .authPin(pin != null ? "****" : null) // No guardar el PIN en claro en auditoría
                .pinVerificado(verificado)
                .build();
        auditoriaRepo.save(auditoria);
    }

    public void verificarAlertas(Medicamento med) {
        // Limpiar alertas anteriores del mismo medicamento
        alertaRepo.findAllByMedicamento(med).forEach(alertaRepo::delete);

        if (med.isStockBajo()) {
            alertaRepo.save(Alerta.builder()
                    .tipo("STOCK_BAJO")
                    .mensaje("Stock crítico para " + med.getNombre() + " (" + med.getStockTotal() + " unid.)")
                    .medicamento(med)
                    .build());
        }

        if (med.isProximoAVencer()) {
            alertaRepo.save(Alerta.builder()
                    .tipo("PROXIMO_VENCIMIENTO")
                    .mensaje("Lote próximo a vencer para " + med.getNombre())
                    .medicamento(med)
                    .build());
        }
    }

    public List<Integer> getConsumoUltimos7Dias() {
        // Simulación: En un sistema real esto consultaría una tabla de Movimientos
        return List.of(45, 52, 38, 65, 48, 22, 15);
    }

    public List<Long> getDistribucionStockPorTipo() {
        // Simulación: Agrupa stock por tipo de medicamento
        return List.of(300L, 150L, 100L);
    }
}
