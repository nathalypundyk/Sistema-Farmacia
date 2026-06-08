package sistema_farmacia.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sistema_farmacia.demo.model.*;
import sistema_farmacia.demo.repository.AlertaRepository;

@Service
public class InventarioService {

    @Autowired private AlertaRepository alertaRepo;

    public void verificarAlertas(Medicamento med) {
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
}
