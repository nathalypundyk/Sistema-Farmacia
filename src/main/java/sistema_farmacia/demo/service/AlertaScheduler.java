package sistema_farmacia.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AlertaScheduler {
    @Autowired
    private WhatsappService whatsappService;

    /**
     * Se ejecuta de lunes a viernes (1-5) a las 11:30 AM.
     * Cron: segundos minutos horas día-mes mes día-semana
     * "MON-FRI" = lunes a viernes solamente.
     */
    @Scheduled(cron = "0 30 11 * * MON-FRI")
    public void enviarAlertaDiaria() {
        System.out.println("⏰ [" + LocalDateTime.now() + "] Ejecutando alerta diaria de WhatsApp...");
        whatsappService.enviarAlertaDiaria();
    }    
}
