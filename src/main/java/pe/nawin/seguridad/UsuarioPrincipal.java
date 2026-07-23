package pe.nawin.seguridad;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import pe.nawin.entidad.Usuario;
import pe.nawin.enumeracion.EstadoUsuario;
import pe.nawin.enumeracion.RolCodigo;

public class UsuarioPrincipal implements UserDetails {

	private final Usuario usuario;

	public UsuarioPrincipal(Usuario usuario) {
		this.usuario = usuario;
	}

	public Usuario usuario() {
		return usuario;
	}

	public Long idUsuario() {
		return usuario.getIdUsuario();
	}

	public RolCodigo rol() {
		return usuario.getRol().getCodigo();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getCodigo().name()));
	}

	@Override
	public String getPassword() {
		return usuario.getClaveHash();
	}

	@Override
	public String getUsername() {
		return usuario.getNombreUsuario();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return usuario.getEstado() != EstadoUsuario.BLOQUEADO;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return usuario.getEstado() == EstadoUsuario.ACTIVO;
	}
}
