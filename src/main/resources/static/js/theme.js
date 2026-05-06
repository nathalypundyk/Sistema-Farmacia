function applyTheme() {
    const hour = new Date().getHours();
    // Modo nocturno entre las 19:00 y las 07:00
    const isNight = hour >= 19 || hour < 7;
    
    if (isNight) {
        document.documentElement.classList.add('dark-mode');
        console.log("Modo nocturno activado automáticamente 🌙");
    } else {
        document.documentElement.classList.remove('dark-mode');
        console.log("Modo diurno activado automáticamente ☀️");
    }
}

// Ejecutar al cargar
document.addEventListener('DOMContentLoaded', applyTheme);

// Verificar cada minuto si debe cambiar el tema
setInterval(applyTheme, 60000);
