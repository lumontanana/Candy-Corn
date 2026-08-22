# Notas de repaso: teoría y sintaxis del proyecto

Recopilación de conceptos de Java/Spring/JPA que han ido apareciendo en el proyecto, pensada para repasar de cara a una entrevista técnica. Cada punto enlaza a dónde se usa en el código real, no son ejemplos de libro.

## 1. Arquitectura en capas

```text
controller -> service -> repository -> entity
```

- **Controller**: solo traduce HTTP <-> DTOs, no contiene lógica de negocio.
- **Service**: casos de uso, transacciones, validación.
- **Repository**: acceso a datos (Spring Data JPA).
- **Entity**: modelo de dominio persistente. Nunca se expone directamente en la API (`ProductResponse`, `OrderResponse`... son DTOs aparte).

Pregunta típica: *"¿por qué no devolver la entidad directamente desde el controller?"* — acoplaría el contrato de la API al modelo de persistencia (cambiar una columna rompería el JSON), y podrías filtrar sin querer relaciones lazy no inicializadas (`LazyInitializationException`).

## 2. Inyección de dependencias sin `@Autowired`

```java
// ProductController.java
private final ProductService productService;

public ProductController(ProductService productService) {
    this.productService = productService;
}
```

Con un único constructor, Spring inyecta automáticamente sin necesidad de `@Autowired`. Usar campos `final` + constructor (en vez de inyección por campo) hace las clases inmutables y testeables sin contenedor Spring (se puede hacer `new ProductService(mock(...))` a pelo, como en los tests).

## 3. Anotaciones REST de Spring MVC

| Anotación | Uso en el proyecto |
|---|---|
| `@RestController` | Combina `@Controller` + `@ResponseBody` (serializa el valor devuelto directamente a JSON) |
| `@RequestMapping("/api/v1/products")` | Prefijo común de rutas a nivel de clase |
| `@GetMapping`, `@PostMapping` | Verbo HTTP + ruta |
| `@PathVariable UUID id` | Extrae de la URL (`/products/{id}`) |
| `@RequestParam(required = false)` / `defaultValue = "0"` | Query params opcionales |
| `@RequestBody CreateOrderRequest request` | Deserializa el body JSON al DTO |

Ejemplo de creación con código de estado explícito ([OrderController.java](../src/main/java/com/candycorn/shop/order/controller/OrderController.java)):

```java
@PostMapping
public ResponseEntity<OrderResponse> create(@RequestBody CreateOrderRequest request) {
    OrderResponse response = orderService.createOrder(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

Sin `ResponseEntity`, Spring devuelve 200 OK por defecto — para un `POST` que crea un recurso lo correcto semánticamente es 201 Created.

## 4. JPA / Hibernate: mapeo de entidades

### Anotaciones básicas ([Product.java](../src/main/java/com/candycorn/shop/catalog/entity/Product.java))

```java
@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(name = "uk_products_slug", columnNames = "slug"))
public class Product {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
```

- `@GeneratedValue` sin estrategia explícita: Hibernate genera UUIDs en memoria (no depende de una secuencia de BD).
- `FetchType.LAZY` en `@ManyToOne`: la categoría no se carga hasta que se accede a `product.getCategory()`. Por defecto `@ManyToOne`/`@OneToOne` son `EAGER` y `@OneToMany`/`@ManyToMany` son `LAZY` — aquí se fuerza `LAZY` explícitamente, buena práctica para evitar el problema N+1.
- Constructor protegido sin argumentos (`protected Product() {}`): lo exige JPA (para crear el proxy/instancia por reflexión) pero se evita que se use desde fuera del paquete.

### Relación bidireccional con agregado ([Order.java](../src/main/java/com/candycorn/shop/order/entity/Order.java))

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderItem> items = new ArrayList<>();
```

- `mappedBy = "order"`: el lado propietario de la relación es `OrderItem.order` (que tiene el `@JoinColumn`); `Order.items` es el lado inverso, no genera columna.
- `cascade = CascadeType.ALL`: guardar/borrar un `Order` guarda/borra sus `OrderItem`.
- `orphanRemoval = true`: si quitas un `OrderItem` de la lista `items`, Hibernate lo borra de la BD aunque no borres el `Order`.
- Encapsulación: `getItems()` devuelve `Collections.unmodifiableList(items)` — no se puede mutar la colección desde fuera, solo a través de `addItem(...)`.

Nota de esta sesión: `@OrderColumn` **no** es compatible con una asociación `mappedBy` (Hibernate lanza un warning); para persistir el orden de una colección inversa se usaría `@OrderBy` en su lugar.

### Embeddable / Value Object ([Address.java](../src/main/java/com/candycorn/shop/order/entity/Address.java), [Order.java](../src/main/java/com/candycorn/shop/order/entity/Order.java))

```java
@Embeddable
public class Address { ... }

// dentro de Order:
@Embedded
@AttributeOverrides({
    @AttributeOverride(name = "recipientName", column = @Column(name = "shipping_recipient_name")),
    ...
})
private Address shippingAddress;
```

`Address` no tiene tabla propia ni `@Id`: sus columnas se "aplanan" dentro de `orders` (`shipping_*`). Es el patrón de **Value Object** de DDD: se identifica por su valor, no por un id, y no tiene sentido consultarlo de forma independiente.

### Enums ([OrderStatus.java](../src/main/java/com/candycorn/shop/order/entity/OrderStatus.java), [Order.java](../src/main/java/com/candycorn/shop/order/entity/Order.java))

```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private OrderStatus status = OrderStatus.PENDING;
```

`EnumType.STRING` guarda el nombre (`"PENDING"`) en vez de `EnumType.ORDINAL` (guardaría `0`, `1`, `2`...). Es casi siempre la opción correcta: si reordenas o insertas un valor en medio del enum, `ORDINAL` corrompe silenciosamente los datos ya guardados.

### Callbacks de ciclo de vida

```java
@PrePersist
void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
}

@PreUpdate
void onUpdate() {
    updatedAt = Instant.now();
}
```

Hibernate los invoca automáticamente antes del `INSERT`/`UPDATE`. Alternativa habitual: `@CreationTimestamp`/`@UpdateTimestamp` de Hibernate, o `AuditingEntityListener` de Spring Data.

## 5. Persistencia: dirty checking vs. `save()` explícito

```java
// OrderService.java — dentro de un método @Transactional
Product product = productRepository.findByIdAndActiveTrue(itemRequest.productId())...;
product.changeStock(product.getStock() - itemRequest.quantity());
// no hace falta productRepository.save(product)
```

`product` es una entidad **gestionada** (managed) porque se obtuvo dentro de la misma transacción, a través del `EntityManager`/repositorio. Hibernate compara su estado al hacer `flush`/`commit` con una copia que guardó al cargarla, y genera el `UPDATE` automáticamente si detecta cambios. Esto es el **dirty checking**. Si el objeto estuviera *detached* (fuera de una transacción, por ejemplo llegado por HTTP), sí haría falta `save()`/`merge()` explícito.

`spring.jpa.open-in-view=false` en [application.properties](../src/main/resources/application.properties): desactiva el patrón "Open Session in View" (que mantiene la sesión de Hibernate abierta durante el renderizado de la vista/serialización). Con `false`, si intentas acceder a una relación `LAZY` fuera de la transacción del service, salta `LazyInitializationException` en vez de disparar queries ocultas — fuerza a que los DTOs se construyan dentro del service, con la sesión todavía abierta.

## 6. `@Transactional`

```java
@Service
@Transactional(readOnly = true)   // por defecto, a nivel de clase
public class OrderService {

    @Transactional               // este método sí escribe, anula el readOnly
    public OrderResponse createOrder(CreateOrderRequest request) { ... }
```

- `readOnly = true` es una pista de optimización: Hibernate puede saltarse el dirty checking y algunos drivers de BD optimizan la transacción sabiendo que no habrá escrituras.
- Anular a nivel de método permite tener casos de uso de lectura y escritura en el mismo service sin repetir la anotación en cada método de solo lectura.
- Si `createOrder` lanza una excepción (p. ej. `InvalidRequestException` por stock insuficiente), Spring hace rollback automático de **todo** lo hecho en la transacción — incluida la reserva de stock de los productos anteriores del mismo pedido. Por defecto solo hace rollback ante `RuntimeException`/`Error` (no ante excepciones *checked*), que es justo lo que se usa aquí (`InvalidRequestException`/`ResourceNotFoundException` extienden `RuntimeException`).

## 7. Spring Data JPA: repositorios

### Métodos derivados por nombre

```java
// ProductRepository.java
Optional<Product> findByIdAndActiveTrue(UUID id);
Page<Product> findAllByCategorySlugAndActiveTrue(String categorySlug, Pageable pageable);
```

Spring Data genera la implementación a partir del nombre del método (`findBy` + campos + operadores como `And`/`True`). Útil para consultas simples; para consultas dinámicas (filtros opcionales) no escala bien — de ahí el punto siguiente.

### Specification API (Criteria API por debajo)

```java
// ProductSpecifications.java
public static Specification<Product> nameContains(String search) {
    return (root, query, builder) -> builder.like(
            builder.lower(root.get("name")),
            "%" + search.toLowerCase(Locale.ROOT) + "%");
}
```

```java
// ProductService.java
var specification = ProductSpecifications.isActive();
if (search != null && !search.isBlank()) {
    specification = specification.and(ProductSpecifications.nameContains(search.trim()));
}
```

`Specification<T>` es una lambda que construye un predicado con la Criteria API (`root`/`query`/`builder`). Se combinan con `.and()`/`.or()` para armar filtros dinámicos sin concatenar SQL a mano ni explotar en un método por cada combinación de filtros. El repositorio tiene que extender `JpaSpecificationExecutor<Product>` para poder usar `findAll(specification, pageable)`.

`Locale.ROOT` en `.toLowerCase(Locale.ROOT)`: evita el ["Turkish locale bug"](https://en.wikipedia.org/wiki/Turkish_I) — en turco, `toLowerCase()` sin locale explícito puede convertir `"I"` en `"ı"` en vez de `"i"`, rompiendo comparaciones. Buena práctica siempre que la comparación no dependa del idioma del usuario.

### Paginación

```java
Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
Page<Product> result = productRepository.findAll(specification, pageable);
```

`Page<T>` incluye contenido + metadatos (`totalElements`, `totalPages`, `number`...). El proyecto lo envuelve en un DTO propio (`PageResponse<T>.from(page)`) para no filtrar el tipo `Page` de Spring Data (que arrastra detalles de Spring) directamente en la respuesta JSON.

## 8. DTOs con `record` (Java 16+)

```java
public record ProductResponse(UUID id, String name, ..., CategoryResponse category) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), ..., CategoryResponse.from(product.getCategory()));
    }
}
```

- Genera constructor, `equals`/`hashCode`/`toString` y accesores automáticamente.
- **Los accesores se llaman como el campo, no `getX()`**: `response.customerName()`, no `response.getCustomerName()`. Es una pregunta trampa habitual.
- Son inmutables (todos los campos son `final`).
- El patrón `static from(entidad)` es un *mapper* manual y explícito — alternativa a usar MapStruct/ModelMapper; aquí se prefiere no añadir esa dependencia por simplicidad y trazabilidad.
- Un record puede anidar otro record como componente y se navega encadenando accesores: `request.shippingAddress().recipientName()`.

## 9. Invariantes de dominio en la entidad (no solo en el service)

```java
// Product.java
public void changeStock(int stock) {
    if (stock < 0) {
        throw new IllegalArgumentException("Stock cannot be negative");
    }
    this.stock = stock;
}
```

```java
// Order.java — máquina de estados con Map<Enum, EnumSet<Enum>>
private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
        OrderStatus.PENDING, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
        OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED),
        ...
        OrderStatus.DELIVERED, EnumSet.noneOf(OrderStatus.class));

public void changeStatus(OrderStatus newStatus) {
    if (!ALLOWED_TRANSITIONS.get(status).contains(newStatus)) {
        throw new IllegalStateException("Cannot move order from " + status + " to " + newStatus);
    }
    status = newStatus;
}
```

Idea central de DDD: una entidad no es una bolsa de getters/setters, protege sus propias reglas (*"tell, don't ask"*). `Map.of(...)` construye un mapa inmutable en una expresión; `EnumSet` es una implementación de `Set` optimizada a nivel de bits para enums (más rápida y compacta que un `HashSet<Enum>`).

Nota: en el service (`OrderService`) se **repite** parte de esta validación (p. ej. cantidad > 0) antes de llegar a la entidad. Es intencional: el service valida en el borde de la API para devolver un `400` con `InvalidRequestException` (mensaje claro al cliente HTTP); la entidad valida el invariante como red de seguridad aunque la llames desde cualquier otro sitio (otro service, un test, batch...), lanzando `IllegalArgumentException`/`IllegalStateException` que no tienen por qué mapear a un código HTTP concreto.

## 10. Snapshot de datos (por qué `OrderItem` copia nombre y precio)

```java
// OrderItem.java
OrderItem(Order order, Product product, int quantity) {
    ...
    this.productName = product.getName();
    this.unitPrice = product.getPrice();
    this.quantity = quantity;
}
```

Un pedido histórico no debe cambiar si luego se sube el precio del producto o se le cambia el nombre. Por eso `OrderItem` no solo guarda una referencia (`product_id`) sino una copia del nombre/precio *en el momento del pedido*. Patrón común en cualquier sistema de facturación/pedidos.

El constructor es **package-private** (sin modificador): solo se puede crear un `OrderItem` desde dentro del paquete `order.entity`, en la práctica solo desde `Order.addItem(...)`. Así se protege el invariante "todo `OrderItem` pertenece a un `Order` y pasó su validación" — es el patrón de **raíz de agregado** (aggregate root) de DDD: no hay `OrderItemRepository`, todo pasa por `Order`.

## 11. Manejo de errores centralizado

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(ResourceNotFoundException exception) {
        ApiError error = new ApiError(Instant.now(), HttpStatus.NOT_FOUND.value(), "RESOURCE_NOT_FOUND", exception.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)   // catch-all, va el último por especificidad
    public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        ...
    }
}
```

`@RestControllerAdvice` intercepta excepciones lanzadas desde cualquier `@RestController` y las centraliza en un solo sitio (en vez de try/catch repetido en cada endpoint). Spring elige el `@ExceptionHandler` más específico para el tipo de excepción lanzada, así que el `Exception.class` genérico actúa de red de seguridad para no filtrar un *stack trace* al cliente (y sí dejarlo en el log del servidor).

`ApiError` es un `record` — el formato de error consistente para toda la API (`timestamp`, `status`, `error`, `message`).

## 12. Testing

### Pirámide de tests usada en el proyecto

| Tipo | Ejemplo | Qué levanta |
|---|---|---|
| Unitario puro | `OrderServiceTest`, `OrderTest`, `ProductTest` | Nada — `new OrderService(mock(...), mock(...))` |
| Slice de controller | `OrderControllerTest`, `ProductControllerTest` | Solo `MockMvc` standalone, sin contexto Spring |
| Integración | `CatalogRepositoryTest` | `@SpringBootTest` completo + H2 en memoria |

### Mockito

```java
private final ProductRepository productRepository = mock(ProductRepository.class);
given(productRepository.findByIdAndActiveTrue(any())).willReturn(Optional.of(product));
given(orderRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
```

- `mock(Clase.class)`: crea un doble de prueba sin comportamiento real.
- `given(...).willReturn(...)` (estilo BDD) es equivalente a `when(...).thenReturn(...)` (estilo clásico) — mismo Mockito, dos APIs para leer mejor en el "given/when/then" de un test.
- `willAnswer(invocation -> invocation.getArgument(0))`: en vez de devolver un valor fijo, devuelve **el mismo argumento** que se le pasó — típico para simular un `save()` que "devuelve lo que le das" (con el id ya asignado en un caso real, aquí simplificado).
- `any()` matcher: acepta cualquier valor de ese tipo; si mezclas matchers y valores literales en la misma llamada, Mockito lanza excepción (todos los argumentos deben ser matchers o ninguno).

### AssertJ

```java
assertThat(response.totalAmount()).isEqualByComparingTo("7.50");
assertThatThrownBy(() -> orderService.createOrder(request))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("customerName is required");
```

- `assertThat(x)` fluido, encadenable, mensajes de fallo más legibles que JUnit puro (`assertEquals`).
- `isEqualByComparingTo` (no `isEqualTo`) para `BigDecimal`: compara por valor numérico, ignorando la escala (`2.50` == `2.500`). Con `isEqualTo` fallaría porque usa `BigDecimal.equals()`, que sí mira la escala.
- `assertThatThrownBy` centraliza capturar-la-excepción-y-comprobarla en una sola expresión fluida, evitando el `try { fail(); } catch { assert }` manual.

### MockMvc

```java
mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setControllerAdvice(new GlobalExceptionHandler())
        .build();

mockMvc.perform(post("/api/v1/orders")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING"));
```

`standaloneSetup` registra un único controller a mano (rápido, no necesita `@SpringBootTest`), pero por eso hay que registrar también el `@RestControllerAdvice` manualmente o las excepciones no se traducirían a JSON de error. `jsonPath("$.campo")` navega la respuesta JSON con sintaxis JSONPath.

### Detalle de esta sesión: Jackson 3

El proyecto usa Spring Boot 4 con **Jackson 3**, cuyo `groupId`/paquete cambió de `com.fasterxml.jackson.*` a `tools.jackson.*` (`tools.jackson.databind.ObjectMapper`). Si repasas Jackson de memoria con ejemplos antiguos, ojo con este detalle específico de la versión.

## 13. Base de datos y migraciones

```sql
-- V3__create_order_tables.sql
CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
CONSTRAINT ck_order_items_quantity_positive CHECK (quantity > 0),
```

- **Flyway**: versiona el esquema con migraciones numeradas (`V1__...`, `V2__...`, `V3__...`) que se aplican una sola vez y en orden; nunca se edita una migración ya aplicada, se añade una nueva.
- `spring.jpa.hibernate.ddl-auto=validate`: Hibernate **no** crea/modifica tablas en producción, solo valida que las entidades coincidan con el esquema real (que gestiona Flyway). En tests (`application-test.properties`) se usa `create-drop` con H2, que sí genera el esquema a partir de las entidades — por eso en tests las migraciones SQL de Flyway están desactivadas (`spring.flyway.enabled=false`) y no hace falta que estén sincronizadas al 100% en cada commit intermedio.
- `CHECK` constraints (`ck_products_stock_non_negative`, `ck_order_items_quantity_positive`) como segunda línea de defensa a nivel de base de datos, además de la validación en Java — por si otra aplicación/proceso escribe directamente en la tabla.
- `ON DELETE CASCADE` en `order_items.order_id`: si se borra un pedido, sus líneas se borran solas a nivel de BD (coherente con `orphanRemoval`/`cascade = ALL` en la entidad `Order`).

## 14. Otros detalles de Java que han salido

- **`UUID` como clave primaria** en vez de `Long`/autoincremental: no revela cuántos registros hay ni el orden de creación, y permite generarlo en el cliente/aplicación sin ir a la BD primero.
- **`BigDecimal` para dinero**, nunca `double`/`float` (errores de redondeo en coma flotante binaria).
- **`Instant`** para timestamps (siempre UTC, sin zona horaria ambigua) en vez de `Date`/`LocalDateTime`.
- **`Optional<T>`** como tipo de retorno de repositorio para modelar "puede no existir" sin `null`.
- **Sobrecarga de métodos privados para reutilizar validación**: `validatePagination(page, size)` extraído y reutilizado por `validateSearchParameters(...)` y por `findAllActiveByCategorySlug(...)` — evita duplicar la misma validación en dos sitios que podrían desincronizarse.
- **`${VARIABLE:default}`** en `application.properties` (`${DB_URL:jdbc:postgresql://localhost:5432/candycorn}`): placeholder de Spring con valor por defecto si la variable de entorno no está definida.
