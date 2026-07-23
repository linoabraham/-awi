-- Tokens FCM por dispositivo (uno por usuario+instalación) y preferencias de
-- notificación por usuario. Base para el envío de push (Fase C).
CREATE TABLE tokens_push (
  id_token_push BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_usuario BIGINT NOT NULL,
  installation_id VARCHAR(64) NOT NULL,
  fcm_token VARCHAR(500) NOT NULL,
  plataforma VARCHAR(20) NULL,
  activo BIT NOT NULL DEFAULT 1,
  fecha_actualizacion DATETIME NOT NULL,
  CONSTRAINT fk_token_push_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario),
  CONSTRAINT uq_token_push_usuario_dispositivo UNIQUE (id_usuario, installation_id)
);

CREATE TABLE preferencias_notificacion (
  id_preferencia BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_usuario BIGINT NOT NULL,
  push BIT NOT NULL DEFAULT 1,
  promos BIT NOT NULL DEFAULT 1,
  pagos BIT NOT NULL DEFAULT 1,
  consultas BIT NOT NULL DEFAULT 1,
  referidos BIT NOT NULL DEFAULT 1,
  seguridad BIT NOT NULL DEFAULT 1,
  fecha_actualizacion DATETIME NOT NULL,
  CONSTRAINT fk_pref_notif_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios (id_usuario),
  CONSTRAINT uq_pref_notif_usuario UNIQUE (id_usuario)
);
