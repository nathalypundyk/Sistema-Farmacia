package sistema_farmacia.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import sistema_farmacia.demo.model.Usuario;
import sistema_farmacia.demo.repository.UsuarioRepository;
import sistema_farmacia.demo.repository.RolRepository;

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
    public String listarUsuarios(Model model) {
        List<Usuario> usuarios = usuarioRepository.findAll();
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("roles", rolRepository.findAll());
        model.addAttribute("activePage", "usuarios");
        return "usuarios";
    }

    @PostMapping("/save")
    public String guardarUsuario(@ModelAttribute Usuario usuario) {
        if (usuario.getId() != null) {
            Optional<Usuario> existente = usuarioRepository.findById(usuario.getId());
            if (existente.isPresent()) {
                Usuario u = existente.get();
                u.setNombre(usuario.getNombre());
                u.setApellido(usuario.getApellido());
                u.setUsername(usuario.getUsername());
                u.setActivo(usuario.getActivo() != null ? usuario.getActivo() : false);
                
                if (usuario.getPassword() != null && !usuario.getPassword().isEmpty()) {
                    u.setPassword(passwordEncoder.encode(usuario.getPassword()));
                }
                if (usuario.getPin() != null && !usuario.getPin().isEmpty()) {
                    u.setPin(passwordEncoder.encode(usuario.getPin()));
                }
                
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
        
        if (usuario.getPin() != null && !usuario.getPin().isEmpty()) {
            usuario.setPin(passwordEncoder.encode(usuario.getPin()));
        }
        
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
