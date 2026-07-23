-- Sesiones diferenciadas por tipo de cliente: WEB (corta) y MOVIL (larga, deslizante).
ALTER TABLE tokens_refresco
    ADD COLUMN tipo_cliente VARCHAR(10) NOT NULL DEFAULT 'WEB' AFTER agente_usuario;
