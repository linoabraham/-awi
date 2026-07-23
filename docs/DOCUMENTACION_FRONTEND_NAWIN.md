# NAWIN - Documentacion frontend por roles

Estado: validado para el backend Spring Boot actual.

Swagger:
- UI general: `GET /swagger-ui.html`
- OpenAPI general: `GET /v3/api-docs`
- Grupos Swagger por modulo:
  - `GET /v3/api-docs/00-autenticacion`
  - `GET /v3/api-docs/10-admin-usuarios`
  - `GET /v3/api-docs/11-admin-clientes`
  - `GET /v3/api-docs/12-admin-catalogo-planes`
  - `GET /v3/api-docs/13-admin-membresias`
  - `GET /v3/api-docs/14-admin-creditos`
  - `GET /v3/api-docs/15-admin-pagos-comprobantes`
  - `GET /v3/api-docs/16-admin-operacion`
  - `GET /v3/api-docs/17-admin-miplata`
  - `GET /v3/api-docs/20-trabajador`
  - `GET /v3/api-docs/25-trabajador-miplata`
  - `GET /v3/api-docs/30-cliente-portal`
  - `GET /v3/api-docs/31-cliente-consultas`
  - `GET /v3/api-docs/32-cliente-miplata`

## 1. Reglas generales frontend

Base URL local:

```text
http://localhost:8080
```

Headers comunes:

```http
Content-Type: application/json
Authorization: Bearer <tokenAcceso>
```

Para operaciones de pago, creditos y consultas se recomienda enviar:

```http
Idempotency-Key: <uuid-generado-por-frontend>
```

El frontend nunca consume CODART directamente. Todas las busquedas pasan por `/api/cliente/consultas/**`. El token CODART vive solo en el backend mediante `CODART_API_TOKEN`.

Respuesta estandar:

```json
{
  "exito": true,
  "mensaje": "Operacion realizada correctamente",
  "datos": {},
  "fechaHora": "2026-07-10T10:30:00-05:00"
}
```

Respuesta de error:

```json
{
  "exito": false,
  "mensaje": "Membresia vencida.",
  "datos": {
    "codigo": "MEM_002",
    "errores": {}
  },
  "fechaHora": "2026-07-10T10:30:00-05:00"
}
```

Codigos principales:

| Codigo | HTTP | Uso frontend |
| --- | ---: | --- |
| AUTH_001 | 401 | Credenciales invalidas. |
| AUTH_002 | 401 | Token vencido o invalido. Renovar token o cerrar sesion. |
| AUTH_003 | 403 | Rol sin permiso o recurso ajeno. |
| USR_001 | 423 | Usuario bloqueado o inactivo. |
| CLI_001 | 404 | Cliente no encontrado. |
| MEM_001 | 403 | No hay membresia activa. |
| MEM_002 | 403 | Membresia vencida. |
| END_001 | 403 | Endpoint no habilitado para la membresia. |
| CUO_001 | 429 | Cuota diaria agotada. |
| CUO_002 | 403 | Cuota del ciclo agotada. |
| CRE_001 | 402 | Creditos insuficientes o saldo MiPlata insuficiente. |
| MFA_001 | 403 | MFA requerido o invalido. |
| CON_001 | 400 | Parametro de consulta invalido. |
| CON_002 | 409 | Consulta duplicada por idempotencia. |
| PRO_001 | 503 | Proveedor temporalmente no disponible. |
| PAG_001 | 409 | Pago no puede ser confirmado. |
| COM_001 | 409 | Comprobante ya emitido. |
| GEN_001 | 400 | Solicitud invalida. |
| GEN_002 | 404 | Recurso no encontrado. |

## 2. Autenticacion comun

Modulo disponible para todos los roles.

| Metodo | Ruta | Body | Respuesta |
| --- | --- | --- | --- |
| POST | `/api/autenticacion/iniciar-sesion` | `IniciarSesionRequest` | `TokenRespuesta` |
| POST | `/api/autenticacion/renovar-token` | `RenovarTokenRequest` | `TokenRespuesta` |
| POST | `/api/autenticacion/cerrar-sesion` | `RenovarTokenRequest` | `{}` |
| GET | `/api/autenticacion/mi-cuenta` | - | `UsuarioActualRespuesta` |
| PATCH | `/api/autenticacion/cambiar-clave` | `CambiarClaveRequest` | `{}` |
| POST | `/api/autenticacion/mfa/configurar` | `MfaConfigurarRequest` | estado MFA |
| POST | `/api/autenticacion/mfa/verificar` | `MfaVerificarRequest` | estado MFA |

Login:

```json
{
  "nombreUsuario": "admin",
  "clave": "Admin@2026"
}
```

Respuesta login:

```json
{
  "tokenAcceso": "jwt",
  "tokenRefresco": "refresh",
  "expiraEnSegundos": 900,
  "usuario": {
    "idUsuario": 1,
    "nombreUsuario": "admin",
    "rol": "ADMIN",
    "estado": "ACTIVO",
    "mfaHabilitado": false
  }
}
```

## 3. ADMIN

El ADMIN configura el sistema, opera clientes y ve auditoria global.

### 3.1 Seguridad y usuarios

| Metodo | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/admin/usuarios` | Crear usuario ADMIN/TRABAJADOR/CLIENTE local. |
| GET | `/api/admin/usuarios` | Listar usuarios. |
| GET | `/api/admin/usuarios/{id}` | Ver usuario. |
| PUT | `/api/admin/usuarios/{id}` | Actualizar rol/datos/estado. |
| DELETE | `/api/admin/usuarios/{id}` | Desactivar usuario. |

`UsuarioCrearRequest`:

```json
{
  "rol": "TRABAJADOR",
  "nombres": "Ana",
  "apellidos": "Quispe Flores",
  "correo": "ana@correo.pe",
  "celular": "987654321",
  "nombreUsuario": "ana.quispe",
  "claveTemporal": "ClaveSegura2026"
}
```

### 3.2 Clientes

| Metodo | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/admin/clientes` | Crear cliente y usuario CLIENTE. |
| GET | `/api/admin/clientes?estado=ACTIVO` | Listar clientes. |
| GET | `/api/admin/clientes/{id}` | Ver cliente. |
| PUT | `/api/admin/clientes/{id}` | Actualizar datos permitidos. |
| DELETE | `/api/admin/clientes/{id}` | Desactivar cliente. |

`ClienteCrearRequest` DNI:

```json
{
  "tipoDocumento": "DNI",
  "numeroDocumento": "12345678",
  "nombres": "Juan Carlos",
  "apellidos": "Perez Lopez",
  "razonSocial": null,
  "correo": "cliente@correo.pe",
  "celular": "987654321",
  "direccion": "Lima",
  "nombreUsuario": "cliente.12345678",
  "claveTemporal": "ClaveSegura2026"
}
```

`ClienteCrearRequest` RUC:

```json
{
  "tipoDocumento": "RUC",
  "numeroDocumento": "20123456789",
  "nombres": null,
  "apellidos": null,
  "razonSocial": "EMPRESA DEMO SAC",
  "correo": "empresa@correo.pe",
  "celular": "987654321",
  "direccion": "Lima",
  "nombreUsuario": "empresa.20123456789",
  "claveTemporal": "ClaveSegura2026"
}
```

### 3.3 Catalogo de busquedas y planes

Catalogo:

| Metodo | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/admin/endpoints-busqueda` | Crear endpoint de catalogo. |
| GET | `/api/admin/endpoints-busqueda?activo=true&critico=false` | Listar catalogo. |
| GET | `/api/admin/endpoints-busqueda/{id}` | Ver endpoint. |
| PUT | `/api/admin/endpoints-busqueda/{id}` | Actualizar endpoint. |
| DELETE | `/api/admin/endpoints-busqueda/{id}` | Inactivar endpoint. |

Planes:

| Metodo | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/admin/planes` | Crear plan. |
| GET | `/api/admin/planes?estado=ACTIVO` | Listar planes. |
| GET | `/api/admin/planes/{id}` | Ver plan. |
| PUT | `/api/admin/planes/{id}` | Actualizar plan. |
| DELETE | `/api/admin/planes/{id}` | Inactivar plan. |

Reglas plan-endpoint:

| Metodo | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/admin/planes/{idPlan}/endpoints` | Agregar endpoint al plan. |
| GET | `/api/admin/planes/{idPlan}/endpoints` | Listar reglas del plan. |
| GET | `/api/admin/planes/{idPlan}/endpoints/{id}` | Ver regla. |
| PUT | `/api/admin/planes/{idPlan}/endpoints/{id}` | Actualizar regla. |
| DELETE | `/api/admin/planes/{idPlan}/endpoints/{id}` | Quitar o deshabilitar regla. |

`PlanRequest`:

```json
{
  "codigo": "MENSUAL_PRO",
  "nombre": "Plan Mensual Pro",
  "descripcion": "Acceso por 30 dias",
  "precioSoles": 79.90,
  "diasVigencia": 30,
  "estado": "ACTIVO"
}
```

`PlanEndpointRequest` incluido por cuota:

```json
{
  "idEndpoint": 1,
  "habilitado": true,
  "modalidadAcceso": "INCLUIDO_MEMBRESIA",
  "limiteDiario": 20,
  "limiteCiclo": 300,
  "costoCreditosCliente": null,
  "requiereMfa": false,
  "requiereFinalidad": false,
  "requiereJustificacion": false,
  "permiteExportar": true,
  "diasRetencion": 30
}
```

`PlanEndpointRequest` por creditos y critico:

```json
{
  "idEndpoint": 12,
  "habilitado": true,
  "modalidadAcceso": "DESCUENTO_CREDITOS",
  "limiteDiario": 5,
  "limiteCiclo": 50,
  "costoCreditosCliente": 30,
  "requiereMfa": true,
  "requiereFinalidad": true,
  "requiereJustificacion": true,
  "permiteExportar": true,
  "diasRetencion": 15
}
```

### 3.4 Membresias

| Metodo | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/admin/membresias` | Crear membresia pendiente. |
| GET | `/api/admin/membresias?cliente=1&estado=ACTIVA` | Listar membresias. |
| GET | `/api/admin/membresias/{id}` | Ver membresia. |
| PUT | `/api/admin/membresias/{id}` | Actualizar pendiente. |
| DELETE | `/api/admin/membresias/{id}` | Cancelar. |
| POST | `/api/admin/membresias/{id}/activar` | Activar con pago confirmado. |
| POST | `/api/admin/membresias/{id}/renovar` | Crear nueva membresia historica. |
| PATCH | `/api/admin/membresias/{id}/suspender` | Suspender. |
| PATCH | `/api/admin/membresias/{id}/reactivar` | Reactivar si no vencio. |
| POST | `/api/admin/membresias/{id}/endpoints` | Agregar acceso efectivo opcional. |
| GET | `/api/admin/membresias/{id}/endpoints` | Listar accesos efectivos. |
| PUT | `/api/admin/membresias/{id}/endpoints/{idMe}` | Actualizar acceso efectivo. |
| DELETE | `/api/admin/membresias/{id}/endpoints/{idMe}` | Deshabilitar acceso. |

`MembresiaCrearRequest`:

```json
{
  "idCliente": 100,
  "idPlan": 3,
  "fechaInicio": "2026-07-15",
  "precioPagado": 79.90,
  "observacion": "Nueva membresia"
}
```

Activar:

```json
{
  "idPago": 300
}
```

Renovar:

```json
{
  "idPlan": 3,
  "fechaInicio": "2026-08-15"
}
```

### 3.5 Creditos

| Metodo | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/admin/paquetes-creditos` | Crear paquete. |
| GET | `/api/admin/paquetes-creditos?activo=true` | Listar paquetes. |
| GET | `/api/admin/paquetes-creditos/{id}` | Ver paquete. |
| PUT | `/api/admin/paquetes-creditos/{id}` | Actualizar paquete. |
| DELETE | `/api/admin/paquetes-creditos/{id}` | Inactivar paquete. |
| POST | `/api/admin/ventas-creditos` | Crear venta. |
| GET | `/api/admin/ventas-creditos?cliente=1&estado=PENDIENTE` | Listar ventas. |
| GET | `/api/admin/ventas-creditos/{id}` | Ver venta. |
| PUT | `/api/admin/ventas-creditos/{id}` | Actualizar venta pendiente. |
| DELETE | `/api/admin/ventas-creditos/{id}` | Anular venta pendiente. |
| GET | `/api/admin/clientes/{id}/saldo-creditos` | Ver saldo. |
| GET | `/api/admin/clientes/{id}/movimientos-creditos` | Ver ledger. |
| POST | `/api/admin/clientes/{id}/ajustes-creditos` | Ajuste o devolucion. |

`PaqueteCreditoRequest`:

```json
{
  "codigo": "CRED_500",
  "nombre": "Paquete 500 creditos",
  "cantidadCreditos": 500,
  "precioSoles": 49.90,
  "diasVigencia": 30,
  "activo": true
}
```

`VentaCreditoRequest`:

```json
{
  "idCliente": 100,
  "idPaqueteCredito": 2,
  "cantidadPaquetes": 1
}
```

`AjusteCreditoRequest`:

```json
{
  "tipo": "AJUSTE",
  "cantidad": 10,
  "motivo": "Ajuste administrativo"
}
```

### 3.6 Pagos y comprobantes

| Metodo | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/admin/pagos` | Registrar pago confirmado; emite comprobante automatico y crea notificacion interna. |
| GET | `/api/admin/pagos?cliente=1&estado=CONFIRMADO` | Listar pagos. |
| GET | `/api/admin/pagos/{id}` | Ver pago. |
| PUT | `/api/admin/pagos/{id}` | Actualizar pago pendiente. |
| DELETE | `/api/admin/pagos/{id}` | Anular pago. |
| POST | `/api/admin/series-comprobantes` | Crear serie. |
| GET | `/api/admin/series-comprobantes?activo=true` | Listar series. |
| GET | `/api/admin/series-comprobantes/{id}` | Ver serie. |
| PUT | `/api/admin/series-comprobantes/{id}` | Actualizar serie. |
| DELETE | `/api/admin/series-comprobantes/{id}` | Inactivar serie. |
| POST | `/api/admin/comprobantes` | Emitir comprobante manual solo si el pago aun no tiene comprobante. |
| GET | `/api/admin/comprobantes?cliente=1` | Listar comprobantes. |
| GET | `/api/admin/comprobantes/{id}` | Ver comprobante. |
| DELETE | `/api/admin/comprobantes/{id}` | Anular comprobante. |
| GET | `/api/admin/comprobantes/{id}/pdf` | Descargar PDF privado. |

`PagoRequest` para membresia:

```json
{
  "idCliente": 100,
  "idMembresia": 200,
  "idVentaCredito": null,
  "montoSoles": 79.90,
  "medioPago": "YAPE",
  "numeroOperacion": "123456789",
  "fechaPago": "2026-07-15T10:30:00",
  "observacion": "Pago verificado"
}
```

`PagoRequest` para creditos:

```json
{
  "idCliente": 100,
  "idMembresia": null,
  "idVentaCredito": 50,
  "montoSoles": 49.90,
  "medioPago": "TRANSFERENCIA",
  "numeroOperacion": "OP-001",
  "fechaPago": "2026-07-15T10:30:00",
  "observacion": "Pago creditos"
}
```

Respuesta de `POST /api/admin/pagos` y `POST /api/trabajador/pagos`:
- El pago queda `CONFIRMADO`.
- Si el pago corresponde a una venta de creditos, el backend carga los creditos automaticamente.
- El backend emite un comprobante automatico. Usa `FACTURA` si el cliente tiene `RUC`, `BOLETA` para `DNI` y cae a `RECIBO_INTERNO` si no hay serie activa del tipo esperado.
- El backend crea una notificacion interna `PAGO_CONFIRMADO` o `CREDITOS_CARGADOS`.
- La respuesta incluye el objeto `comprobante`.

`ComprobanteEmitirRequest`:

```json
{
  "idPago": 300,
  "idSerieComprobante": 1,
  "tipoComprobante": "BOLETA"
}
```

### 3.7 Operacion, reportes y auditoria

| Metodo | Ruta | Uso |
| --- | --- | --- |
| GET | `/api/admin/consultas` | Historial global. |
| GET | `/api/admin/consultas/{codigo}` | Detalle global auditado. |
| POST | `/api/admin/notificaciones` | Crear notificacion. |
| GET | `/api/admin/notificaciones?cliente=1&estado=PENDIENTE` | Listar notificaciones. |
| GET | `/api/admin/notificaciones/{id}` | Ver notificacion. |
| PUT | `/api/admin/notificaciones/{id}` | Actualizar pendiente. |
| DELETE | `/api/admin/notificaciones/{id}` | Cancelar pendiente. |
| GET | `/api/admin/reportes/resumen` | Dashboard resumen. |
| GET | `/api/admin/reportes/ventas` | Reporte ventas. |
| GET | `/api/admin/reportes/membresias` | Reporte membresias. |
| GET | `/api/admin/reportes/consultas` | Reporte consultas. |
| GET | `/api/admin/auditorias?entidad=consultas` | Auditoria. |

### 3.8 MiPlata

MiPlata es la billetera digital del cliente. Todo monto se maneja en soles (`PEN` / `S/.`). ADMIN puede revisar recargas, aprobarlas, rechazarlas y auditar los movimientos.

| Metodo | Ruta | Uso |
| --- | --- | --- |
| GET | `/api/admin/miplata/solicitudes-recarga?cliente=100&estado=PENDIENTE` | Listar solicitudes de recarga. |
| GET | `/api/admin/miplata/solicitudes-recarga/{id}` | Ver solicitud con comprobante base64. |
| POST | `/api/admin/miplata/solicitudes-recarga/{id}/aprobar` | Aprobar recarga y acreditar saldo. |
| POST | `/api/admin/miplata/solicitudes-recarga/{id}/rechazar` | Rechazar recarga con motivo. |
| GET | `/api/admin/miplata/clientes/{idCliente}/billetera` | Ver saldo MiPlata del cliente. |
| GET | `/api/admin/miplata/clientes/{idCliente}/movimientos` | Ver historial completo de MiPlata. |

Estados de solicitud:

```text
PENDIENTE, APROBADA, RECHAZADA
```

Rechazar recarga:

```json
{
  "motivo": "Comprobante ilegible o monto no coincide"
}
```

Al aprobar una recarga:
- Se acredita `montoSoles` en la billetera del cliente.
- Si `codigoReferidoIngresado` existe y pertenece a otro cliente, se acredita un bono fijo de `3.00` soles al cliente referido.
- Se registran movimientos `RECARGA_APROBADA` y, cuando corresponde, `BONO_REFERIDO`.
- Se registra un pago interno confirmado con `medioPago = TRANSFERENCIA`.
- Se emite un comprobante automatico y queda disponible en `/api/{rol}/comprobantes` y `/api/cliente/comprobantes`.
- Se crea notificacion interna `RECARGA_MIPLATA` para el cliente; si hubo bono, tambien se notifica al referido.

Al rechazar una recarga:
- No se acredita saldo.
- No se entrega bono de referido.
- Se conserva `motivoRechazo` para auditoria.
- Se registra un movimiento `RECARGA_RECHAZADA` con el saldo sin cambios.
- Se crea notificacion interna `RECARGA_MIPLATA` con el motivo de rechazo.

## 4. TRABAJADOR

El TRABAJADOR opera clientes, membresias, ventas, pagos, comprobantes y renovaciones. No configura catalogo, planes globales ni usuarios.

### 4.1 Clientes

| Metodo | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/trabajador/clientes` | Crear cliente. |
| GET | `/api/trabajador/clientes?estado=ACTIVO` | Listar clientes. |
| GET | `/api/trabajador/clientes/{id}` | Ver cliente. |
| PUT | `/api/trabajador/clientes/{id}` | Actualizar cliente. |
| DELETE | `/api/trabajador/clientes/{id}` | Desactivar cliente. |

### 4.2 Membresias

| Metodo | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/trabajador/membresias` | Crear membresia. |
| GET | `/api/trabajador/membresias?cliente=1&estado=ACTIVA` | Listar membresias. |
| GET | `/api/trabajador/membresias/{id}` | Ver membresia. |
| PUT | `/api/trabajador/membresias/{id}` | Actualizar pendiente. |
| DELETE | `/api/trabajador/membresias/{id}` | Cancelar pendiente. |
| POST | `/api/trabajador/membresias/{id}/activar` | Activar con pago. |
| POST | `/api/trabajador/membresias/{id}/renovar` | Renovar. |

### 4.3 Creditos

| Metodo | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/trabajador/ventas-creditos` | Crear venta de creditos. |
| GET | `/api/trabajador/ventas-creditos?cliente=1&estado=PENDIENTE` | Listar ventas. |
| GET | `/api/trabajador/ventas-creditos/{id}` | Ver venta. |
| PUT | `/api/trabajador/ventas-creditos/{id}` | Actualizar venta pendiente. |
| DELETE | `/api/trabajador/ventas-creditos/{id}` | Anular venta pendiente. |
| GET | `/api/trabajador/clientes/{id}/saldo-creditos` | Ver saldo del cliente. |

### 4.4 Pagos y comprobantes

| Metodo | Ruta | Uso |
| --- | --- | --- |
| POST | `/api/trabajador/pagos` | Registrar pago confirmado; emite comprobante automatico y crea notificacion interna. |
| GET | `/api/trabajador/pagos?cliente=1&estado=CONFIRMADO` | Listar pagos. |
| GET | `/api/trabajador/pagos/{id}` | Ver pago. |
| PUT | `/api/trabajador/pagos/{id}` | Actualizar pendiente. |
| DELETE | `/api/trabajador/pagos/{id}` | Anular pendiente. |
| POST | `/api/trabajador/comprobantes` | Emitir comprobante manual solo si el pago aun no tiene comprobante. |
| GET | `/api/trabajador/comprobantes?cliente=1` | Listar comprobantes. |
| GET | `/api/trabajador/comprobantes/{id}` | Ver comprobante. |
| DELETE | `/api/trabajador/comprobantes/{id}` | Anular/solicitar anulacion. |
| GET | `/api/trabajador/comprobantes/{id}/pdf` | Descargar PDF. |

### 4.5 Renovaciones

| Metodo | Ruta | Uso |
| --- | --- | --- |
| GET | `/api/trabajador/renovaciones/por-vencer?dias=10` | Clientes por vencer. |
| GET | `/api/trabajador/renovaciones/vencidas` | Clientes vencidos. |
| POST | `/api/trabajador/notificaciones-renovacion` | Registrar notificacion. |
| GET | `/api/trabajador/notificaciones-renovacion?cliente=1&estado=ENVIADA` | Listar notificaciones. |

### 4.6 MiPlata

TRABAJADOR puede revisar recargas y consultar billeteras igual que ADMIN, sin configurar catalogos ni planes.

| Metodo | Ruta | Uso |
| --- | --- | --- |
| GET | `/api/trabajador/miplata/solicitudes-recarga?cliente=100&estado=PENDIENTE` | Listar solicitudes de recarga. |
| GET | `/api/trabajador/miplata/solicitudes-recarga/{id}` | Ver solicitud con comprobante base64. |
| POST | `/api/trabajador/miplata/solicitudes-recarga/{id}/aprobar` | Aprobar recarga y acreditar saldo. |
| POST | `/api/trabajador/miplata/solicitudes-recarga/{id}/rechazar` | Rechazar recarga con motivo. |
| GET | `/api/trabajador/miplata/clientes/{idCliente}/billetera` | Ver saldo MiPlata del cliente. |
| GET | `/api/trabajador/miplata/clientes/{idCliente}/movimientos` | Ver historial completo de MiPlata. |

El body para rechazo es el mismo de ADMIN:

```json
{
  "motivo": "Pago no identificado"
}
```

## 5. CLIENTE

El CLIENTE solo ve y opera sus propios datos.

### 5.1 Inicio, perfil y estado

| Metodo | Ruta | Uso |
| --- | --- | --- |
| GET | `/api/cliente/inicio` | Resumen: membresia, dias restantes, saldo y endpoints. |
| GET | `/api/cliente/perfil` | Ver perfil. |
| PUT | `/api/cliente/perfil` | Actualizar correo, celular y direccion. |
| GET | `/api/cliente/membresia` | Membresia actual. |
| GET | `/api/cliente/membresias` | Historial de membresias. |
| GET | `/api/cliente/endpoints-disponibles` | Endpoints habilitados efectivos. |
| GET | `/api/cliente/creditos/saldo` | Saldo de creditos. |
| GET | `/api/cliente/creditos/movimientos` | Movimientos propios. |
| GET | `/api/cliente/comprobantes` | Comprobantes propios. |
| GET | `/api/cliente/comprobantes/{id}` | Ver comprobante propio. |
| GET | `/api/cliente/comprobantes/{id}/pdf` | Descargar PDF propio. |
| GET | `/api/cliente/notificaciones` | Notificaciones propias. |
| PATCH | `/api/cliente/notificaciones/{id}/leer` | Marcar leida. |

`PerfilClienteRequest`:

```json
{
  "correo": "cliente@correo.pe",
  "celular": "987654321",
  "direccion": "Lima"
}
```

### 5.2 Consultas de busqueda

Reglas antes de llamar:
- Usuario autenticado y rol `CLIENTE`.
- Cliente `ACTIVO`.
- Membresia `ACTIVA` y fecha actual dentro de vigencia.
- Endpoint habilitado en `membresias_endpoints`.
- Si modalidad `INCLUIDO_MEMBRESIA`: consume cuota.
- Si modalidad `DESCUENTO_CREDITOS`: consume creditos.
- Si el acceso efectivo del endpoint indica `requiereFinalidad`, `requiereJustificacion` o `requiereMfa`, el front debe enviar esos campos.
- Si esas banderas estan en `false`, el backend permite consultar sin MFA, finalidad ni justificacion aunque el endpoint este marcado como critico en catalogo.
- Si proveedor falla tecnicamente: se libera reserva y no cobra.

Headers recomendados:

```http
Authorization: Bearer <tokenAcceso>
Idempotency-Key: <uuid>
```

Endpoints no criticos:

| Codigo | Metodo | Ruta | Body |
| --- | --- | --- | --- |
| RUC | POST | `/api/cliente/consultas/ruc` | `{"ruc":"20123456789"}` |
| DNI_BASICO | POST | `/api/cliente/consultas/dni-basico` | `{"dni":"12345678"}` |
| DNIV | POST | `/api/cliente/consultas/dniv` | `{"dni":"12345678"}` |
| DNIVEL | POST | `/api/cliente/consultas/dnivel` | `{"dni":"12345678"}` |
| DNI_COMPLETO | POST | `/api/cliente/consultas/dni-completo` | `{"dni":"12345678"}` |
| NM | POST | `/api/cliente/consultas/nombres` | `{"nombres":"JUAN","apellidoPaterno":"PEREZ","apellidoMaterno":"LOPEZ"}` |
| AG | POST | `/api/cliente/consultas/familiares` | `{"dni":"12345678"}` |
| PLA | POST | `/api/cliente/consultas/placa` | `{"placa":"ABC123"}` |
| PLAT | POST | `/api/cliente/consultas/vehiculo-propietarios` | `{"placa":"ABC123"}` |
| HSOAT | POST | `/api/cliente/consultas/historial-soat` | `{"placa":"ABC123"}` |

Endpoints criticos:

| Codigo | Metodo | Ruta | Body |
| --- | --- | --- | --- |
| DNIT | POST | `/api/cliente/consultas/dnit` | `{"dni":"12345678","finalidad":"Verificacion autorizada","justificacion":"Contrato vigente","codigoMfa":"123456"}` |
| TELP_DNI | POST | `/api/cliente/consultas/telefonos-por-dni` | `{"dni":"12345678","finalidad":"...","justificacion":"...","codigoMfa":"123456"}` |
| TELP_CEL | POST | `/api/cliente/consultas/titular-celular` | `{"numero":"987654321","finalidad":"...","justificacion":"...","codigoMfa":"123456"}` |
| DEN | POST | `/api/cliente/consultas/denuncias-resumen` | `{"dni":"12345678","finalidad":"...","justificacion":"...","codigoMfa":"123456"}` |
| DENUNCIAS | POST | `/api/cliente/consultas/denuncias-pdf` | `{"dni":"12345678","finalidad":"...","justificacion":"...","codigoMfa":"123456"}` |
| RQH | POST | `/api/cliente/consultas/requisitorias` | `{"dni":"12345678","finalidad":"...","justificacion":"...","codigoMfa":"123456"}` |
| DENPLA | POST | `/api/cliente/consultas/denuncias-placa` | `{"placa":"ABC123","finalidad":"...","justificacion":"...","codigoMfa":"123456"}` |

En los endpoints criticos, `finalidad`, `justificacion` y `codigoMfa` son condicionales:
- Enviar `finalidad` solo si `requiereFinalidad = true`.
- Enviar `justificacion` solo si `requiereJustificacion = true`.
- Enviar `codigoMfa` solo si `requiereMfa = true`.

FACIAL_TOP usa multipart:

```http
POST /api/cliente/consultas/facial-top
Content-Type: multipart/form-data
Authorization: Bearer <tokenAcceso>
Idempotency-Key: <uuid>
```

Campos:

| Campo | Tipo | Regla |
| --- | --- | --- |
| image_facial | file | jpg, jpeg o png; maximo 5MB |
| finalidad | string | obligatorio solo si `requiereFinalidad = true` |
| justificacion | string | obligatorio solo si `requiereJustificacion = true` |
| codigoMfa | string | obligatorio solo si `requiereMfa = true` |

Historial:

| Metodo | Ruta | Uso |
| --- | --- | --- |
| GET | `/api/cliente/consultas` | Historial propio. |
| GET | `/api/cliente/consultas/{codigo}` | Detalle propio. |
| DELETE | `/api/cliente/consultas/{codigo}` | Ocultar del historial propio. |
| GET | `/api/cliente/consultas/{codigo}/archivos/{id}` | Descargar archivo si pertenece al cliente, no expiro y permite exportar. |

Respuesta de consulta:

```json
{
  "codigoConsulta": "uuid",
  "estado": "EXITOSA",
  "origenConsumo": "CREDITOS",
  "cantidadConsumida": 30,
  "permiteExportar": true,
  "resultado": {},
  "archivos": [
    {
      "idArchivoConsulta": 1,
      "nombreArchivo": "archivo-1.pdf",
      "tipoArchivo": "PDF",
      "tipoMime": "application/pdf",
      "tamanoBytes": 12345
    }
  ],
  "fecha": "2026-07-10T10:30:00"
}
```

### 5.3 MiPlata

El CLIENTE opera solo su propia billetera. El saldo siempre esta en soles (`PEN` / `S/.`) y el codigo de referido propio llega en la billetera y en las respuestas del cliente como `codigoReferido`.

| Metodo | Ruta | Uso |
| --- | --- | --- |
| GET | `/api/cliente/miplata/billetera` | Ver saldo disponible, moneda y codigo de referido propio. |
| GET | `/api/cliente/miplata/movimientos` | Ver movimientos propios de MiPlata. |
| POST | `/api/cliente/miplata/recargas` | Solicitar recarga con comprobante base64. |
| GET | `/api/cliente/miplata/recargas` | Ver solicitudes propias de recarga. |
| GET | `/api/cliente/miplata/paquetes-creditos` | Listar paquetes de creditos activos para comprar con saldo. |
| GET | `/api/cliente/miplata/planes` | Listar planes activos para comprar con saldo. |
| POST | `/api/cliente/miplata/compras/creditos` | Comprar creditos descontando saldo MiPlata; genera pago, comprobante y notificacion. |
| POST | `/api/cliente/miplata/compras/planes` | Comprar plan mensual descontando saldo MiPlata; genera pago, comprobante y notificacion. |

Solicitar recarga:

```json
{
  "montoSoles": 50.00,
  "comprobanteBase64": "data:image/png;base64,iVBORw0KGgoAAA...",
  "codigoReferido": "NAW12345678"
}
```

Reglas de recarga:
- `montoSoles` debe ser mayor a cero.
- `comprobanteBase64` es obligatorio y debe ser base64 valido; puede incluir prefijo `data:*;base64,`.
- `codigoReferido` es opcional.
- Si el codigo no existe, la recarga continua normal y no hay bono.
- El bono de referido de `3.00` soles solo se entrega cuando la recarga pasa de `PENDIENTE` a `APROBADA`.
- Al solicitar recarga se crea una notificacion interna `RECARGA_MIPLATA` en estado pendiente de revision.
- Al aprobar recarga se crea pago interno, comprobante automatico y notificacion de aprobacion. Al rechazar se notifica el motivo.

Comprar creditos:

```json
{
  "idPaqueteCredito": 2,
  "cantidadPaquetes": 1
}
```

Comprar plan:

```json
{
  "idPlan": 3,
  "fechaInicio": "2026-07-15"
}
```

Si `fechaInicio` es `null`, el backend usa la fecha actual. Si no hay saldo suficiente para creditos o planes, devuelve `CRE_001` con HTTP 402.

Respuesta de compra MiPlata:
- Siempre incluye `billetera` con el saldo actualizado.
- Compra de creditos incluye `ventaCredito`, `pago` confirmado con `medioPago = MIPLATA` y `comprobante`.
- Compra de plan incluye `membresia`, `pago` confirmado con `medioPago = MIPLATA` y `comprobante`.
- El comprobante tambien queda disponible en `GET /api/cliente/comprobantes`.
- El backend crea notificacion interna `CREDITOS_CARGADOS` para creditos o `COMPRA_MIPLATA` para planes.

Respuesta de billetera:

```json
{
  "idBilleteraMiPlata": 1,
  "idCliente": 100,
  "saldoDisponible": 53.00,
  "moneda": "PEN",
  "codigoReferido": "NAW12345678",
  "fechaActualizacion": "2026-07-12T10:30:00"
}
```

Movimiento MiPlata:

```json
{
  "idMovimientoMiPlata": 10,
  "idCliente": 100,
  "tipoMovimiento": "RECARGA_APROBADA",
  "montoSoles": 50.00,
  "saldoAnterior": 0.00,
  "saldoPosterior": 50.00,
  "moneda": "PEN",
  "descripcion": "Recarga MiPlata aprobada",
  "idSolicitudRecargaMiPlata": 5,
  "idVentaCredito": null,
  "idMembresia": null,
  "fechaCreacion": "2026-07-12T10:30:00"
}
```

Tipos de movimiento:

```text
RECARGA_APROBADA, RECARGA_RECHAZADA, BONO_REFERIDO, COMPRA_CREDITOS, COMPRA_PLAN, AJUSTE, DEVOLUCION
```

## 6. Flujos frontend recomendados

### 6.1 Alta de cliente con membresia

1. ADMIN/TRABAJADOR crea cliente: `POST /api/{rol}/clientes`.
2. ADMIN crea plan y reglas si no existen: `/api/admin/planes` y `/api/admin/planes/{idPlan}/endpoints`.
3. ADMIN/TRABAJADOR crea membresia pendiente: `POST /api/{rol}/membresias`.
4. ADMIN/TRABAJADOR registra pago: `POST /api/{rol}/pagos`; el backend emite comprobante y notificacion interna.
5. ADMIN/TRABAJADOR activa membresia: `POST /api/{rol}/membresias/{id}/activar`.
6. Cliente inicia sesion y consulta `GET /api/cliente/inicio`.

### 6.2 Venta de creditos

1. ADMIN crea paquete: `POST /api/admin/paquetes-creditos`.
2. ADMIN/TRABAJADOR crea venta: `POST /api/{rol}/ventas-creditos`.
3. ADMIN/TRABAJADOR registra pago con `idVentaCredito`.
4. Backend carga saldo, emite comprobante automatico y crea notificacion interna `CREDITOS_CARGADOS`.
5. Cliente consulta saldo en `GET /api/cliente/creditos/saldo`.

### 6.3 Consulta cliente

1. Cliente obtiene endpoints disponibles: `GET /api/cliente/endpoints-disponibles`.
2. UI muestra solo endpoints habilitados.
3. UI solicita finalidad, justificacion o MFA solo segun las banderas efectivas del endpoint.
4. UI envia `Idempotency-Key`.
5. Backend reserva cuota/creditos, consulta CODART, guarda resultado cifrado y confirma consumo.
6. Si CODART falla tecnicamente, backend libera reserva y devuelve `PRO_001`.

### 6.4 Recarga y compra con MiPlata

1. Cliente consulta `GET /api/cliente/miplata/billetera` para ver saldo y `codigoReferido`.
2. Cliente envia `POST /api/cliente/miplata/recargas` con monto, comprobante base64 y codigo referido opcional.
3. ADMIN/TRABAJADOR revisa la solicitud en `/api/{rol}/miplata/solicitudes-recarga`.
4. Si aprueba, el backend acredita saldo en soles, registra `RECARGA_APROBADA`, crea pago interno, emite comprobante y notifica al cliente.
5. Si el codigo referido es valido y no es del mismo cliente, el backend acredita `BONO_REFERIDO` por `3.00` soles al referido y lo notifica.
6. Si rechaza, el backend guarda `motivoRechazo`, registra `RECARGA_RECHAZADA`, no modifica el saldo y notifica al cliente.
7. Cliente compra creditos o planes desde `/api/cliente/miplata/compras/**`; el backend descuenta MiPlata, registra `COMPRA_CREDITOS` o `COMPRA_PLAN`, crea pago `MIPLATA`, emite comprobante y notifica.

## 7. Validaciones importantes para UI

Documentos:
- DNI: exactamente 8 digitos.
- RUC: exactamente 11 digitos.
- Celular: 9 digitos para consultas telefonicas.
- Placa: 6 o 7 caracteres alfanumericos.

Estados:
- Usuario: `ACTIVO`, `BLOQUEADO`, `INACTIVO`.
- Cliente: `ACTIVO`, `INACTIVO`.
- Plan: `BORRADOR`, `ACTIVO`, `INACTIVO`.
- Membresia: `PENDIENTE`, `ACTIVA`, `VENCIDA`, `SUSPENDIDA`, `CANCELADA`.
- Pago: `PENDIENTE`, `CONFIRMADO`, `ANULADO`.
- MedioPago: `EFECTIVO`, `YAPE`, `PLIN`, `TRANSFERENCIA`, `TARJETA`, `MIPLATA`.
- Consulta: `PENDIENTE`, `PROCESANDO`, `EXITOSA`, `SIN_RESULTADOS`, `FALLIDA`, `RECHAZADA`.
- Solicitud MiPlata: `PENDIENTE`, `APROBADA`, `RECHAZADA`.
- Notificacion: `BIENVENIDA`, `POR_VENCER`, `VENCIDA`, `RENOVADA`, `PAGO_CONFIRMADO`, `CREDITOS_CARGADOS`, `RECARGA_MIPLATA`, `COMPRA_MIPLATA`.
- Moneda del sistema: siempre soles, `PEN` / `S/.`.

Reglas de UI:
- No mostrar busquedas si no hay membresia activa.
- No permitir endpoints ausentes de `endpoints-disponibles`.
- Mostrar saldo de creditos antes de endpoints `DESCUENTO_CREDITOS`.
- Mostrar saldo MiPlata antes de compras de creditos o planes.
- Para recargas MiPlata, exigir monto mayor a cero y comprobante base64.
- No bloquear la recarga si el codigo referido ingresado no existe; el backend simplemente no otorga bono.
- Mostrar cuotas/limites cuando `modalidadAcceso` sea `INCLUIDO_MEMBRESIA`.
- Para `MFA_001`, reabrir solo los campos marcados como requeridos en el endpoint efectivo: MFA, finalidad o justificacion.
- Para `MEM_002`, dirigir al flujo de renovacion.
- Para `CRE_001`, dirigir al flujo de compra de creditos o recarga MiPlata, segun la pantalla.

## 8. Variables backend que debe conocer despliegue

Estas variables son de backend. El frontend no debe recibir sus valores.

```text
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
JWT_SECRET
JWT_ACCESS_MINUTOS
JWT_REFRESH_HORAS_WEB
JWT_REFRESH_DIAS_MOVIL
CODART_BASE_URL
CODART_API_TOKEN
CIFRADO_DATOS_KEY
ALMACENAMIENTO_PRIVADO_RUTA
FRONTEND_ORIGIN
ZONA_HORARIA
```
