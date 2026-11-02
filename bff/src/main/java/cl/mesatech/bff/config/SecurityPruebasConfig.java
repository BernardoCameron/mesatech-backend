package cl.mesatech.bff.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * SOLO PARA PRUEBAS LOCALES (perfil "sinauth"): no valida ningun token.
 * Cada request entra como un usuario de prueba; el rol se elige con el
 * header X-Test-Rol (Cliente, Operador o Administrador; por defecto
 * Administrador) y el usuario con X-Test-Usuario.
 * Nunca activar este perfil en EC2.
 */
@Configuration
@Profile("sinauth")
public class SecurityPruebasConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityPruebasConfig.class);

    @Bean
    public SecurityFilterChain filterChainSinAuth(HttpSecurity http,
                                                  @Qualifier("corsConfigurationSource") CorsConfigurationSource cors) throws Exception {
        logger.warn("PERFIL sinauth ACTIVO: el BFF NO esta validando tokens (solo para pruebas locales)");

        http
                .csrf(csrf -> csrf.disable())
                .cors(c -> c.configurationSource(cors))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new UsuarioPruebaFilter(), AnonymousAuthenticationFilter.class);

        return http.build();
    }

    // Arma un JWT falso con los mismos claims que usan los controllers (oid, name, roles)
    private static class UsuarioPruebaFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                        FilterChain chain) throws ServletException, IOException {
            String rol = valor(request, "X-Test-Rol", "Administrador");
            String usuario = valor(request, "X-Test-Usuario", "usuario-prueba");

            Jwt jwt = Jwt.withTokenValue("sinauth")
                    .header("alg", "none")
                    .claim("oid", usuario)
                    .claim("name", valor(request, "X-Test-Nombre", usuario))
                    .claim("roles", List.of(rol))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();

            SecurityContextHolder.getContext().setAuthentication(
                    new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));

            chain.doFilter(request, response);
        }

        private String valor(HttpServletRequest request, String header, String porDefecto) {
            String valor = request.getHeader(header);
            return (valor == null || valor.isBlank()) ? porDefecto : valor;
        }
    }
}
