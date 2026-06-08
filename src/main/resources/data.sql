-- ============================================================
-- TIPOS DE MEDICAMENTO
-- ============================================================
INSERT INTO tipo_medicamento (nombre, descripcion) VALUES ('Antibiotico', 'Infecciones bacterianas');
INSERT INTO tipo_medicamento (nombre, descripcion) VALUES ('Analgesico', 'Dolor y fiebre');
INSERT INTO tipo_medicamento (nombre, descripcion) VALUES ('Controlado', 'Psicotropicos y estupefacientes');
INSERT INTO tipo_medicamento (nombre, descripcion) VALUES ('Antihipertensivo', 'Presion arterial');
INSERT INTO tipo_medicamento (nombre, descripcion) VALUES ('Antiulceroso', 'Protectores gastricos');
INSERT INTO tipo_medicamento (nombre, descripcion) VALUES ('Hipolipemiante', 'Colesterol y lipidos');
INSERT INTO tipo_medicamento (nombre, descripcion) VALUES ('Vitaminas', 'Suplementos vitaminicos');
INSERT INTO tipo_medicamento (nombre, descripcion) VALUES ('Antirretroviral', 'VIH/SIDA');
INSERT INTO tipo_medicamento (nombre, descripcion) VALUES ('Antiviral', 'Infecciones virales');
INSERT INTO tipo_medicamento (nombre, descripcion) VALUES ('Hematologico', 'Trastornos sanguineos');

-- ============================================================
-- PRESENTACIONES
-- ============================================================
INSERT INTO presentacion (nombre, unidades) VALUES ('Comprimidos', 30);
INSERT INTO presentacion (nombre, unidades) VALUES ('Ampolla', 10);
INSERT INTO presentacion (nombre, unidades) VALUES ('Capsula', 30);
INSERT INTO presentacion (nombre, unidades) VALUES ('Inyectable', 1);
INSERT INTO presentacion (nombre, unidades) VALUES ('Frasco', 1);

-- ============================================================
-- ROLES
-- ============================================================
INSERT INTO roles (nombre, descripcion) VALUES ('ADMIN', 'Administrador Total');
INSERT INTO roles (nombre, descripcion) VALUES ('FARMACEUTICO', 'Gestion de Farmacia');
INSERT INTO roles (nombre, descripcion) VALUES ('ENFERMERO', 'Registro de Consumos');

-- ============================================================
-- USUARIOS
-- admin / admin123  |  farma / farma123  |  nurse / nurse123
-- ============================================================
INSERT INTO usuarios (username, password, nombre, apellido, pin, activo)
VALUES ('admin', '$2a$10$.dvSAi0IWtSdFl9l87tbI.nEh5G9994.6h2SWV65bDlxqfA4dyRce', 'Mario', 'Administrador', '1111', true);
INSERT INTO usuarios (username, password, nombre, apellido, pin, activo)
VALUES ('farma', '$2a$10$hfU4fPmcEBYPiB3YcMRMmeEXDlIKgmVDa1Rk0tfXNfOor1EG2Cyuy', 'Lucia', 'Farmaceutica', '2222', true);
INSERT INTO usuarios (username, password, nombre, apellido, pin, activo)
VALUES ('nurse', '$2a$10$MBPe9rCpqEdGNEVO0ax7FOSFo/ZkrhiJtPO3wtE.9oKN2Pnhj26MG', 'Pedro', 'Enfermero', '3333', true);

INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (1, 1);
INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (2, 2);
INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (3, 3);
