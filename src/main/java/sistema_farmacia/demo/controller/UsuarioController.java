package sistema_farmacia.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sistema_farmacia.demo.model.Usuario;
import sistema_farmacia.demo.repository.UsuarioRepository;
import sistema_farmacia.demo.repository.RolRepository;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public String listarUsuarios(Model model, Principal principal) {
        List<Usuario> usuarios = usuarioRepository.findAll();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("roles", rolRepository.findAll());
        model.addAttribute("activePage", "usuarios");
        // Para que la vista pueda bloquear el cambio de contraseña sobre la propia cuenta
        usuarioRepository.findByUsername(principal.getName())
                .ifPresent(u -> model.addAttribute("currentUserId", u.getId()));
        return "usuarios";
    }

    @PostMapping("/save")
    public String guardarUsuario(@ModelAttribute Usuario usuario, Principal principal) {
        if (usuario.getId() != null) {
            Optional<Usuario> existente = usuarioRepository.findById(usuario.getId());
            if (existente.isPresent()) {
                Usuario u = existente.get();
                boolean esMiPropiaCuenta = u.getUsername() != null
                        && u.getUsername().equals(principal.getName());
                u.setNombre(usuario.getNombre());
                u.setApellido(usuario.getApellido());
                u.setUsername(usuario.getUsername());
                u.setActivo(usuario.getActivo() != null ? usuario.getActivo() : false);

                // Un admin NO puede cambiar su propia contraseña desde acá: debe usar
                // "Cambiar mi contraseña" (autoservicio con contraseña actual).
                // Sí puede RESETEAR la de otros usuarios (recuperación de cuenta bloqueada).
                if (!esMiPropiaCuenta
                        && usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
                    u.setPassword(passwordEncoder.encode(usuario.getPassword()));
                }
                // El PIN NO se gestiona aquí: cada usuario cambia el suyo en "Cambiar mi PIN"
                // (autoservicio con contraseña). Ver DashboardController.cambiarPin.

                if(usuario.getRoles() != null && !usuario.getRoles().isEmpty()){
                    u.setRoles(usuario.getRoles());
                }
                
                usuarioRepository.save(u);
                return "redirect:/usuarios?success";
            }
        }
        
        if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        } else {
            usuario.setPassword(passwordEncoder.encode("123456")); // default
        }
        // El usuario nuevo arranca sin PIN; lo configura él mismo en "Cambiar mi PIN".

        if (usuario.getActivo() == null) {
            usuario.setActivo(true);
        }
        usuarioRepository.save(usuario);
        return "redirect:/usuarios?success";
    }

    @PostMapping("/delete")
    public String eliminarUsuario(@RequestParam Long id) {
        usuarioRepository.deleteById(id);
        return "redirect:/usuarios?success";
    }
}
