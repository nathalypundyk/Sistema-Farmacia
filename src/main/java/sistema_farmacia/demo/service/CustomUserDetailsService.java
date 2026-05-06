package sistema_farmacia.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import sistema_farmacia.demo.repository.UsuarioRepository;

@Service
@Primary
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("DEBUG: Intentando cargar usuario: " + username);
        return usuarioRepository.findByUsername(username)
                .map(user -> {
                    System.out.println("DEBUG: Usuario encontrado: " + user.getUsername() + " con roles: " + user.getRoles());
                    return user;
                })
                .orElseThrow(() -> {
                    System.out.println("DEBUG: Usuario no encontrado: " + username);
                    return new UsernameNotFoundException("Usuario no encontrado: " + username);
                });
    }
}
