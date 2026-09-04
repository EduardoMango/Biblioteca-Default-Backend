# **Spring Security JWT**

## Implementación paso a paso

1. # **Importando dependencias**

|       \<dependency\>        	\<groupId\>org.springframework.boot\</groupId\>        	\<artifactId\>spring-boot-starter-security\</artifactId\>    	\</dependency\>    	\<dependency\>        	\<groupId\>io.jsonwebtoken\</groupId\>        	\<artifactId\>jjwt-api\</artifactId\>        	\<version\>0.13.0\</version\>             \<scope\>compile\</scope\>    	\</dependency\>    	\<dependency\>        	\<groupId\>io.jsonwebtoken\</groupId\>        	\<artifactId\>jjwt-impl\</artifactId\>        	\<version\>0.13.0\</version\>             \<scope\>runtime\</scope\>    	\</dependency\>    	\<dependency\>        	\<groupId\>io.jsonwebtoken\</groupId\>        	\<artifactId\>jjwt-jackson\</artifactId\>        	\<version\>0.13.0\</version\>             \<scope\>runtime\</scope\>    	\</dependency\> |
| :---- |

2. # **Roles y permisos**

Es el momento de definir nuestros roles y permisos. Cada rol, es un conjunto de permisos que el usuario puede realizar. Para esto se utilizan tanto Enum para definir los posibles valores, como entidades, ya que  estos se persisten en la base de datos.

| public enum Permits {	VER\_CUENTAS,	CREAR\_CUENTA,	ACTUALIZAR\_CUENTA,	ELIMINAR\_CUENTA,	VER\_USUARIOS,	CREAR\_USUARIO,	ACTUALIZAR\_USUARIO,	ELIMINAR\_USUARIO} |
| :---- |

| @Entity@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builderpublic class PermitEntity {	@Id	@GeneratedValue(strategy \= GenerationType.IDENTITY)	Long id;	@Enumerated(EnumType.STRING)	@Column(nullable \= false, unique \= true)	Permits permit;} |
| :---- |

| public enum Roles {	ROLE\_USER,	ROLE\_ADMIN} |
| :---- |

| @Entity@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builderpublic class RoleEntity {    	@Id    	@GeneratedValue(strategy \= GenerationType.IDENTITY)    	private Long id;    	@Enumerated(EnumType.STRING)    	@Column(nullable \= false, unique \= true)    	private Roles role;	@ManyToMany(cascade \= CascadeType.MERGE, fetch \= FetchType.EAGER)	@JoinTable(        	name \= "role\_permits",        	joinColumns \= @JoinColumn(name \= "role\_id"),        	inverseJoinColumns \= @JoinColumn(name \= "permit\_id"))	private final Set\<PermitEntity\> permits \= new HashSet\<\>();	public RoleEntity(Roles name) {    	this.role \= name;	}	public void addPermit(PermitEntity permit) {    	this.permits.add(permit);	}} |
| :---- |

	Si Permit y Role son Entities, estas deben entonces tener ***Repositories*** asociados.

3. # **Credenciales & UserDetails**

UserDetails es una interfaz que contiene los metodos requeridos por Spring Security para la validacion de un usuario. Como buena practica, se suele crear una clase CredentialsEntity que implemente esta interfaz y me otorgue los metodos necesarios. Tambien es la clase encargada de otorgarnos los permisos para la autorizacion de usuarios.  
	

| @Entity@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builderpublic class CredentialsEntity implements UserDetails {    @Id    @GeneratedValue(strategy \= jakarta.persistence.GenerationType.IDENTITY)    private Long id;    @Column(unique \= true,nullable \= false)    private String username;    @Column(nullable \= false)    private String password;    @Column(nullable \= false, columnDefinition \= "boolean default true")    private Boolean enabled;    @OneToOne    @JoinColumn(name \= "usuario\_id", referencedColumnName \= "id", unique \= true)    private UserEntity usuario;    @ManyToMany(cascade \= CascadeType.MERGE,fetch \= FetchType.EAGER)    @JoinTable(            name \= "credentials\_roles",            joinColumns \= @JoinColumn(name \= "credential\_id"),            inverseJoinColumns \= @JoinColumn(name \= "role\_id")    )    private Set\<RoleEntity\> roles \= new HashSet\<\>();    @Override    public Collection\<? extends GrantedAuthority\> getAuthorities() {        Set\<GrantedAuthority\> authorities \= new HashSet\<\>();        roles.forEach(rol \-\> authorities.add(                new SimpleGrantedAuthority(rol.getRole().name())));        return authorities;    }    @Override    public String getPassword() {        return this.password;    }    @Override    public String getUsername() {        return this.username;    }    @Override    public boolean isEnabled() {        return Boolean.TRUE.equals(this.enabled);    }} |
| :---- |

La interfaz UserDetails contiene los siguientes metodos

* **getAuthorities:** Devuelve una colección de GrantedAuthority, que define los

permisos o roles del usuario. GrantedAuthority es un objeto que representa una autoridad en el sistema, es decir, un permiso especifico.

* **getUsername y getPassword**: Proporcionan el nombre de usuario y la contraseña,

que serán usados por Spring Security para verificar las credenciales en el proceso  
de autenticación.

* **isAccountNonExpired, isAccountNonLocked, isCredentialsNonExpired, isEnabled:** Estos métodos indican si la cuenta está activa, si las credenciales no han caducado, etc.

4. # **JwtService**

JwtService es el servicio encargado de crear y validar tokens JWT.

| @Servicepublic class JwtService {    @Value("${jwt.secret}")    private String jwtSecretKey;    @Value("${jwt.expiration}")    private Long jwtExpiration;    public String extractUsername(String token) {        return extractClaim(token, Claims::getSubject);    }    public String generateToken(UserDetails userDetails) {        Map\<String, Object\> claims \= new HashMap\<\>();        List\<String\> roles \= userDetails.getAuthorities().stream()                .map(GrantedAuthority::getAuthority)                .collect(Collectors.toList());        claims.put("roles", roles);        return buildToken(claims, userDetails, jwtExpiration);    }    public List\<GrantedAuthority\> extractAuthorities(String token) {        Claims claims \= extractAllClaims(token);        List\<?\> rawRoles \= claims.get("roles", List.class);        if (rawRoles \== null) {            return java.util.Collections.emptyList();        }        return rawRoles.stream()                .map(Object::toString)                .map(SimpleGrantedAuthority::new)                .collect(Collectors.toList());    }    private \<T\> T extractClaim(String token, Function\<Claims, T\> claimsResolver) {        final Claims claims \= extractAllClaims(token);        return claimsResolver.apply(claims);    }    private Claims extractAllClaims(String token) {        return Jwts                .parser()                .verifyWith(getSignInKey())                .build()                .parseSignedClaims(token)                .getPayload();    }    public boolean isTokenValid(String token, UserDetails userDetails) {        final String username \= extractUsername(token);        return (username.equals(userDetails.getUsername()))                && \!isTokenExpired(token)                && userDetails.isAccountNonLocked()                && userDetails.isEnabled();    }    private String buildToken(            Map\<String, Object\> extraClaims,            UserDetails userDetails,            long expiration    ) {        return Jwts                .builder().claims(extraClaims)                .subject(userDetails.getUsername())                .issuedAt(new Date(System.currentTimeMillis()))                .expiration(new Date(System.currentTimeMillis() \+ expiration))                .signWith(getSignInKey())                .compact();    }    private SecretKey getSignInKey() {        byte\[\] keyBytes \= this.jwtSecretKey.getBytes(StandardCharsets.UTF\_8);        return Keys.hmacShaKeyFor(keyBytes);    }    private boolean isTokenExpired(String token) {        Date expiration \= extractClaim(token, Claims::getExpiration);        return expiration.before(new Date());    }} |
| :---- |

* **`jwtExpiration -`** Representa la duracion del token en milisegundos.  
* **`jwtSecret -`** Es un String aleatorio, normalmente de una alta longitud, que se utiliza para firmar y autenticar los tokens. Esta informacion es sensible ya que permite generar tokens para nuestro sistema. Puede generarse en el siguiente enlace: https://jwtsecret.com/generate  
* **`extractUsername`** \- Obtiene el nombre de usuario (subject) de un token JWT.  
* **`generateToken`** \- Crea un nuevo token JWT para un usuario, incluyendo sus roles como "claims" adicionales.  
* **`extractClaim`** \- Es un método auxiliar que extrae una "claim" específica de un token, aplicando una función resolutora.  
* **`extractAllClaims`** \- Es un método auxiliar que parsea el token JWT y devuelve todas las "claims" (declaraciones) contenidas en su cuerpo.  
* **`isTokenValid`** \- Verifica si un token JWT es válido para un usuario dado, comprobando el nombre de usuario, la caducidad, y el estado de la cuenta del usuario.  
* **`buildToken`** \- Es un método auxiliar que construye y firma el token JWT, añadiendo las "claims" extras, el sujeto, la fecha de emisión y la fecha de expiración.  
* **`getSignInKey`** \- Decodifica la clave secreta configurada (`jwtSecretKey`) y la convierte en un objeto Secret`Key` para la firma y verificación de tokens.  
* **`isTokenExpired`** \- Comprueba si la fecha de expiración de un token JWT ya ha pasado.

5. # **UserDetailsService**

Spring Security usa la interfaz UserDetailsService para cargar los datos del usuario desde una fuente de datos, en este caso, una base de datos. Al implementar UserDetailsService en JpaUserDetailsService, le decimos a Spring cómo obtener la información de los usuarios mediante JPA.

| @Service@RequiredArgsConstructorpublic class UserDetailsServiceImpl implements UserDetailsService {    private final CredentialsRepository credentialsRepository;    @Override    public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {        return credentialsRepository.findByUsername(username).orElseThrow(() \-\> new UsernameNotFoundException("User not found"));    }} |
| :---- |

6. # **JwtAuthenticationFilter**

JwtAuthenticationFilter hereda de OncePerRequestFilter, una clase de Spring que garantiza que el filtro se ejecute una sola vez por solicitud. Este filtro es responsable de interceptar cada solicitud que llega al servidor y verificar si contiene un token JWT en su encabezado de autorización.

| @Component@RequiredArgsConstructorpublic class JwtAuthenticationFilter extends OncePerRequestFilter {    private final JwtService jwtService;    @Override    protected void doFilterInternal(HttpServletRequest request,                                    HttpServletResponse response,                                    FilterChain filterChain) throws ServletException, IOException {        final String authHeader \= request.getHeader("Authorization");        if (authHeader \== null || \!authHeader.startsWith("Bearer "))        {            filterChain.doFilter(request, response);            return;        }        final String jwt \= authHeader.substring(7);        try {            final String username \= jwtService.extractUsername(jwt);            Authentication authentication \=                    SecurityContextHolder.getContext().getAuthentication();            if (username \!= null && authentication \== null){                List\<GrantedAuthority\> authorities \= jwtService.extractAuthorities(jwt);                UsernamePasswordAuthenticationToken authToken \= new                        UsernamePasswordAuthenticationToken(                        username,                        null,                        authorities                );                authToken.setDetails(new                        WebAuthenticationDetailsSource().buildDetails(request));                SecurityContextHolder.getContext().setAuthentication(authToken);            }        } catch (JwtException e) {            response.setStatus(HttpServletResponse.SC\_UNAUTHORIZED);            response.setContentType("application/json");            response.getWriter().write(String.format("{\\"error\\": \\"Token JWT invalido o expirado\\", \\"status\\": %d, \\"path\\": \\"%s\\"}",                    HttpServletResponse.SC\_UNAUTHORIZED, request.getRequestURI()));            response.getWriter().flush();            return;        }        filterChain.doFilter(request, response);    }} |
| :---- |

7. # **SecurityConfig**

SecurityConfig es una clase donde colocaremos toda la configuracion relacionada a lo que es seguridad. Por ahora, solo vamos a declarar dos beans que necesitaremos mas adelante, PasswordEncoder (Para la encriptación de contraseñas) y AuthenticationManager para la validacion de credenciales.  
	Mas adelante declararemos aqui toda la configuracion de endpoints, cuales son privados, cuales publicos y que permisos requieren.

| @Configurationpublic class SecurityConfig {	@Bean	public PasswordEncoder passwordEncoder() {    	return new BCryptPasswordEncoder();	}	@Bean	public AuthenticationManager authenticationManager(AuthenticationConfiguration config)        	throws Exception {    	return config.getAuthenticationManager();	}} |
| :---- |

8. # **AuthService**

Servicio encargado de la autenticacion inicial del usuario. AuthService utiliza el AuthenticationManager de Spring Security para verificar las credenciales.

	Importante \- No se olviden de declarar el bean de AuthenticationManager y de crear el dto AuthRequest.

| @Service@RequiredArgsConstructorpublic class AuthService {    private final CredentialsRepository credentialsRepository;    private final AuthenticationManager authenticationManager;    public UserDetails authenticate(AuthRequest input) {        authenticationManager.authenticate(                new UsernamePasswordAuthenticationToken(                        input.username(),                        input.password()                )        );        return credentialsRepository.findByUsername(input.username()).orElseThrow(                () \-\> new UsernameNotFoundException("Usuario no encontrado")        );    }    } |
| :---- |

9. # **AuthController**

	Este sera nuestro controlador con los endpoints para autenticación. Desde aquí gestionaremos las peticiones y devolveremos los tokens.

| @RestController@RequestMapping("/api/auth")@RequiredArgsConstructorpublic class AuthController {    private final AuthService authService;    private final UserService userService;    private final JwtService jwtService;    @PostMapping("/login")    public ResponseEntity\<AuthResponse\> authenticateUser(@RequestBody AuthRequest authRequest){        UserDetails user \= authService.authenticate(authRequest);        String token \= jwtService.generateToken(user);        return ResponseEntity.ok(new AuthResponse(token));    }    @PostMapping("/register")    public ResponseEntity\<UserDTO\> registerUser(@RequestBody NewAccountRequest newAccountRequest){        return new ResponseEntity\<\>(userService.save(newAccountRequest), HttpStatus.CREATED);    }} |
| :---- |

10. # **Configurando Seguridad**

Con todo esto establecido, ya tenemos implementado todo lo  relativo a tokens y gestion de permisos y usuarios. Pero seguimos con seguridad basica. Para modificar eso, es necesario sobreescribir la configuracion de seguridad de Spring Security e implementar una propia. Esto lo podemos agregar a la clase SecurityConfig.

	

| @Beanpublic SecurityFilterChain filterChain (HttpSecurity http) throws Exception {        http.authorizeHttpRequests(auth \-\> auth                        .requestMatchers("/api/auth/\*\*").permitAll()                        .anyRequest().authenticated())                .cors(Customizer.withDefaults())                .csrf(AbstractHttpConfigurer::disable)                .headers(headers \-\>headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))                .sessionManagement(manager \-\> manager.sessionCreationPolicy(STATELESS))                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)                .exceptionHandling(e \-\>                        e.authenticationEntryPoint(restAuthenticationEntryPoint));        return http.build();    } |
| :---- |

11. # **Manejo de excepciones**

Si bien ya tenemos Spring Security funcionando con tokens JWT (Si todo salio bien), no estamos manejando ninguna de las excepciones que pueden lanzarse durante el proceso. Como ya sabemos, Spring Boot las manejara por nosotros aunque nosotros no las manejemos, pero es recomendable darle un manejo personalizado para poder responderle al usuario de manera mas precisa porque fallo la autenticacion. Esto se hace a traves de la interfaz AuthenticationEntryPoint, la cual nos otorga un metodo commence al que le sera pasada la excepcion para que la maneje.

| @Componentpublic class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {    @Override    public void commence(HttpServletRequest request,                         HttpServletResponse response,                         AuthenticationException authException) throws IOException, ServletException {        response.setContentType("application/json");        response.setStatus(HttpServletResponse.SC\_UNAUTHORIZED);        String errorMessage \= switch (authException) {            case BadCredentialsException badCredentialsException \-\>                    "Credenciales inválidas";            case DisabledException disabledException \-\>                    "Cuenta deshabilitada";            case LockedException lockedException \-\>                    "Cuenta bloqueada";            case AccountExpiredException accountExpiredException \-\>                    "Cuenta expirada";            case CredentialsExpiredException credentialsExpiredException \-\>                    "Credenciales expiradas";            case InsufficientAuthenticationException insufficientAuthenticationException \-\>                    "Autenticación insuficiente";            case AuthenticationServiceException authenticationServiceException \-\>                    "Error en el servicio de autenticación";            default \-\> "Error de autenticación: " \+ authException.getMessage();        };        ObjectMapper mapper \= new ObjectMapper();        Map\<String, Object\> responseData \= new HashMap\<\>();        responseData.put("error", errorMessage);        responseData.put("status", HttpServletResponse.SC\_UNAUTHORIZED);        responseData.put("path", request.getRequestURI());        response.getWriter().write(mapper.writeValueAsString(responseData));        response.getWriter().flush();    }} |
| :---- |

# **Refresh Token**

Una opcion bastante estandar y recomendable a la hora de trabajar con tokens JWT, es la opcion de refrescar tokens sin requerir enviar nuevamente las credenciales. Esto se hace tambien a traves de un token JWT, que se llama comunmente Refresh Token o token de refresco. Este token tiene una duracion mucho mas extensa que un token tipico y me ahorra el reenviar credenciales, lo cual vuelve nuestra autenticacion mas segura. Vamos a ver que se requiere agregar a nuestra implementacion previa para que nuestra API soporte el uso de este tipo de tokens.

1. # **Agregar Refresh Token a las credenciales**

|    @Column(name \= "refresh\_token",length \= 2048,unique \= true, nullable \= false)    private String refreshToken; |
| :---- |

Es comun que este token de refresco se almacene en la base de datos del usuario, para facilitar su acceso a futuro. Esto tambien nos permite, si se desease, bloquear un refresh token de ser utilizado, ya que podemos eliminarlo o reemplazarlo en la base de datos y el antiguo token, aunque no haya vencido, deja de ser valido para nuestro sistema.

2. # **Modificar AuthResponse**

| public record AuthResponse(String AccessToken,String refreshToken) {} |
| :---- |

	Modificamos nuestro AuthResponse, para incluir un refreshToken para el usuario.

3. # **Modificar JWTService**

| public String generateRefreshToken(UserDetails userDetails) {        Map\<String, Object\> claims \= new HashMap\<\>();        claims.put("type", "refresh");        return buildToken(claims, userDetails, refreshTokenExpiration);    }    public boolean validateRefreshToken(String refreshToken, UserDetails userDetails) {        try {            Jwts.parserBuilder()                    .setSigningKey(getSignInKey())                    .build()                    .parseClaimsJws(refreshToken);            final String username \= extractUsername(refreshToken);            return (username.equals(userDetails.getUsername())) &&                    \!isTokenExpired(refreshToken);        } catch (JwtException e) {            return false; // Invalid token        }    } |
| :---- |

	  
Se agregan dos metodos, generateRefreshToken, que modifica el claim para solamente agregar el type:refresh como claim y el metodo validateRefreshtoken, que es muy similar al previo, solamente que no valida otros aspectos como estado de la cuenta, usuario al que pertenece, etc.

4. # **Modificar AuthService**

| @Transactional    public AuthResponse refreshAccessToken(String refreshToken) {        String username \= jwtService.extractUsername(refreshToken);        CredentialsEntity user \= credentialsRepository.findByEmail(username)                .orElseThrow(() \-\> new IllegalArgumentException("User not found"));        if (\!user.getRefreshToken().equals(refreshToken)) {            throw new IllegalArgumentException("Refresh token does not match");        }        if (\!jwtService.validateRefreshToken(refreshToken, user)) {            throw new IllegalArgumentException("Refresh token expired or invalid");        }        String newAccessToken \= jwtService.generateToken(user);        String newRefreshToken \= jwtService.generateRefreshToken(user);        user.setRefreshToken(newRefreshToken);        credentialsRepository.save(user);        return new AuthResponse(newAccessToken, newRefreshToken);    } |
| :---- |

5. # **Agregando endpoint**

|   @PostMapping()    public ResponseEntity\<AuthResponse\> authenticateUser(@RequestBody AuthRequest authRequest){        CredentialsEntity user \= authService.authenticate(authRequest);        System.out.println(user);        String token \= jwtService.generateToken(user);        System.out.println(token);        return ResponseEntity.ok(new AuthResponse(token, user.getRefreshToken()));    }    @PostMapping("/refresh")    public ResponseEntity\<AuthResponse\> refreshToken(@RequestBody RefreshTokenRequest request){        AuthResponse response \= authService.refreshAccessToken(request.refreshToken());        return ResponseEntity.ok(response);    } |
| :---- |

