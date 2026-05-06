-- Insertar Tipos
INSERT INTO tipo_medicamento (nombre, descripcion) VALUES ('Antibiotico', 'Infecciones');
INSERT INTO tipo_medicamento (nombre, descripcion) VALUES ('Analgesico', 'Dolor');
INSERT INTO tipo_medicamento (nombre, descripcion) VALUES ('Controlado', 'Psicotropicos');

-- Insertar Presentaciones
INSERT INTO presentacion (nombre, unidades) VALUES ('Comprimidos', 30);
INSERT INTO presentacion (nombre, unidades) VALUES ('Ampolla', 10);

-- Insertar Medicamentos (Gran Hospital del Sur)
-- Ibuprofeno (No crítico)
INSERT INTO medicamento (codigo, nombre, es_prioridad, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister) 
VALUES ('IBU-400', 'Ibuprofeno 400mg', false, 20, 100, 200, 2, 1, 10);

-- Fentanilo (Crítico - Requiere PIN)
INSERT INTO medicamento (codigo, nombre, es_prioridad, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister) 
VALUES ('FEN-05', 'Fentanilo 0.05mg', true, 5, 20, 50, 3, 2, 1);

-- Amoxicilina
INSERT INTO medicamento (codigo, nombre, es_prioridad, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister) 
VALUES ('AMO-500', 'Amoxicilina 500mg', false, 15, 50, 100, 1, 1, 8);

-- Insertar Lotes (FEFO Test)
-- Lote que vence pronto
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id) 
VALUES ('LOTE-A1', '2026-06-01', 50, 'DISPONIBLE', 1);

-- Lote que vence despues
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id) 
VALUES ('LOTE-A2', '2026-12-01', 100, 'DISPONIBLE', 1);

-- Lote para Fentanilo (Stock bajo para activar alerta)
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id) 
VALUES ('LOTE-F1', '2026-08-01', 3, 'DISPONIBLE', 2);

-- Insertar Roles
INSERT INTO roles (nombre, descripcion) VALUES ('ADMIN', 'Administrador Total');
INSERT INTO roles (nombre, descripcion) VALUES ('FARMACEUTICO', 'Gestión de Farmacia');
INSERT INTO roles (nombre, descripcion) VALUES ('ENFERMERO', 'Registro de Consumos');

-- Insertar Usuarios (password: password)
INSERT INTO usuarios (username, password, nombre, apellido, pin, activo) 
VALUES ('admin', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', 'Mario', 'Admin', '1111', true);
INSERT INTO usuarios (username, password, nombre, apellido, pin, activo) 
VALUES ('farma', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', 'Lucia', 'Farma', '2222', true);
INSERT INTO usuarios (username, password, nombre, apellido, pin, activo) 
VALUES ('nurse', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.TVuHOn2', 'Pedro', 'Nurse', '3333', true);

-- Asignar Roles
INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (1, 1);
INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (2, 2);
INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (3, 3);
