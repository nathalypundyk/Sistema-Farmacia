-- ============================================================
-- TIPOS DE MEDICAMENTO (id 1-10)
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
-- PRESENTACIONES (id 1-5)
-- ============================================================
INSERT INTO presentacion (nombre, unidades) VALUES ('Comprimidos', 30);
INSERT INTO presentacion (nombre, unidades) VALUES ('Ampolla', 10);
INSERT INTO presentacion (nombre, unidades) VALUES ('Capsula', 30);
INSERT INTO presentacion (nombre, unidades) VALUES ('Inyectable', 1);
INSERT INTO presentacion (nombre, unidades) VALUES ('Frasco', 1);

-- ============================================================
-- MEDICAMENTOS (basados en IMI Hospital General de Itapua)
-- id 1: IBU-400  id 2: FEN-05  id 3: AMO-500 ... id 4-18 nuevos
-- ============================================================

-- id=1: Ibuprofeno 400mg (LASA con 600mg)
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('IBU-400', 'Ibuprofeno', '400 MG', false, true, 'Ibuprofeno 600 MG', 200, 554, 900, 2, 1, 10);

-- id=2: Fentanilo (Controlado - Requiere PIN, LASA)
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('FEN-05', 'Fentanilo', '0.05 MG', true, true, 'Fentanilo 0.1 MG', 10, 30, 50, 3, 2, 1);

-- id=3: Amoxicilina
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('AMO-500', 'Amoxicilina', '500 MG', false, false, null, 80, 150, 200, 1, 1, 8);

-- id=4: Paracetamol (alto consumo)
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('PAR-500', 'Paracetamol (Acetaminofeno)', '500 MG', false, false, null, 500, 700, 1000, 2, 1, 10);

-- id=5: Losartan Potasico (LASA - 50mg vs 100mg)
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('LOS-50', 'Losartan (Potasico)', '50 MG', false, true, 'Losartan 100 MG', 600, 800, 1200, 4, 1, 10);

-- id=6: Diclofenac Potasico
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('DIC-50', 'Diclofenac Potasico', '50 MG', false, false, null, 650, 850, 1000, 2, 1, 10);

-- id=7: Omeprazol
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('OMP-20', 'Omeprazol', '20 MG', false, false, null, 440, 600, 800, 5, 3, 10);

-- id=8: Atorvastatina (LASA - 40mg vs 20mg)
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('ATR-40', 'Atorvastatina', '40 MG', false, true, 'Atorvastatina 20 MG', 400, 550, 700, 6, 1, 10);

-- id=9: Amlodipina
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('AML-10', 'Amlodipina (Besilato)', '10 MG', false, false, null, 200, 400, 600, 4, 1, 10);

-- id=10: Acido Folico 5mg
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('FOL-5', 'Acido Folico', '5 MG', false, false, null, 90, 200, 350, 7, 1, 10);

-- id=11: Sulfato Ferroso + Acido Folico
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('FEF-125', 'Sulfato Ferroso + Acido Folico', '125 MG + 0.5 MG', false, false, null, 180, 250, 350, 10, 1, 10);

-- id=12: Aciclovir 400mg
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('ACI-400', 'Aciclovir', '400 MG', false, false, null, 35, 100, 200, 9, 1, 10);

-- id=13: Midazolam (Controlado, LASA, CRITICO en demo)
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('MID-5', 'Midazolam', '5 MG/ML', true, true, 'Midazolam 15 MG/3 ML', 730, 900, 1200, 3, 4, 1);

-- id=14: Tenofovir/Lamivudina/Dolutegravir (antirretroviral, CRITICO en demo)
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('TNF-300', 'Tenofovir / Lamivudina / Dolutegravir', '300 MG / 300 MG / 50 MG', false, false, null, 260, 350, 500, 8, 5, 1);

-- id=15: Calcio Carbonato (CRITICO en demo)
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('CAL-500', 'Calcio (Carbonato)', '500 MG', false, false, null, 465, 600, 800, 7, 1, 10);

-- id=16: Acetato de Medroxiprogesterona (Controlado, CRITICO)
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('MED-150', 'Acetato de Medroxiprogesterona', '150 MG', true, false, null, 50, 100, 150, 3, 2, 1);

-- id=17: Acido Acetilsalicilico 125mg
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('ASA-125', 'Acido Acetil Salicilico', '125 MG', false, false, null, 180, 300, 500, 2, 1, 10);

-- id=18: Acido Valproico (Controlado, LASA, AGOTADO en demo)
INSERT INTO medicamento (codigo, nombre, concentracion, es_prioridad, es_lasa, similara, minimo, deseable, recomendado, tipo_id, presentacion_id, unidades_por_blister)
VALUES ('VPA-250', 'Acido Valproico (Valproato Sodico)', '250 MG/5 ML', true, true, 'Acido Valproico 500 MG/5 ML', 25, 60, 100, 3, 5, 1);

-- ============================================================
-- LOTES (stock real basado en IMI)
-- ============================================================
-- Ibuprofeno: lote vencido pronto + lote OK
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-IBU-01', '2026-06-20', 80, 'DISPONIBLE', 1);
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-IBU-02', '2026-12-01', 651, 'DISPONIBLE', 1);

-- Fentanilo: stock bajo critico
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-FEN-01', '2026-08-01', 3, 'DISPONIBLE', 2);

-- Amoxicilina
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-AMO-01', '2027-03-01', 150, 'DISPONIBLE', 3);

-- Paracetamol (stock OK alto)
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-PAR-01', '2027-06-01', 947, 'DISPONIBLE', 4);

-- Losartan (stock OK, overstocked)
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-LOS-01', '2027-04-01', 1375, 'DISPONIBLE', 5);

-- Diclofenac (stock OK, casi critico)
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-DIC-01', '2026-10-01', 689, 'DISPONIBLE', 6);

-- Omeprazol
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-OMP-01', '2027-01-01', 501, 'DISPONIBLE', 7);

-- Atorvastatina
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-ATR-01', '2027-02-01', 598, 'DISPONIBLE', 8);

-- Amlodipina
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-AML-01', '2027-05-01', 644, 'DISPONIBLE', 9);

-- Acido Folico
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-FOL-01', '2026-11-01', 281, 'DISPONIBLE', 10);

-- Sulfato Ferroso
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-FEF-01', '2027-01-01', 320, 'DISPONIBLE', 11);

-- Aciclovir
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-ACI-01', '2027-03-01', 344, 'DISPONIBLE', 12);

-- Midazolam (CRITICO: stock 467 < minimo 730)
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-MID-01', '2026-09-01', 467, 'DISPONIBLE', 13);

-- Tenofovir (CRITICO: stock 239 < minimo 260)
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-TNF-01', '2026-07-15', 239, 'DISPONIBLE', 14);

-- Calcio Carbonato (CRITICO: stock 413 < minimo 465)
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-CAL-01', '2027-02-01', 413, 'DISPONIBLE', 15);

-- Medroxiprogesterona (CRITICO: stock 28 < minimo 50)
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-MED-01', '2026-07-01', 28, 'DISPONIBLE', 16);

-- Acido Acetilsalicilico
INSERT INTO lotes (nro_lote, fecha_vencimiento, cantidad_disponible, estado, medicamento_id)
VALUES ('LOTE-ASA-01', '2027-04-01', 958, 'DISPONIBLE', 17);

-- Acido Valproico (AGOTADO: sin lotes con stock)
-- sin lote -> stockTotal = 0 -> estado AGOTADO

-- ============================================================
-- ROLES
-- ============================================================
INSERT INTO roles (nombre, descripcion) VALUES ('ADMIN', 'Administrador Total');
INSERT INTO roles (nombre, descripcion) VALUES ('FARMACEUTICO', 'Gestión de Farmacia');
INSERT INTO roles (nombre, descripcion) VALUES ('ENFERMERO', 'Registro de Consumos');

-- ============================================================
-- USUARIOS (admin:admin123 / farma:farma123 / nurse:nurse123)
-- ============================================================
INSERT INTO usuarios (username, password, nombre, apellido, pin, activo)
VALUES ('admin', '$2a$10$.dvSAi0IWtSdFl9l87tbI.nEh5G9994.6h2SWV65bDlxqfA4dyRce', 'Mario', 'Administrador', '1111', true);
INSERT INTO usuarios (username, password, nombre, apellido, pin, activo)
VALUES ('farma', '$2a$10$hfU4fPmcEBYPiB3YcMRMmeEXDlIKgmVDa1Rk0tfXNfOor1EG2Cyuy', 'Lucia', 'Farmaceutica', '2222', true);
INSERT INTO usuarios (username, password, nombre, apellido, pin, activo)
VALUES ('nurse', '$2a$10$MBPe9rCpqEdGNEVO0ax7FOSFo/ZkrhiJtPO3wtE.9oKN2Pnhj26MG', 'Pedro', 'Enfermero', '3333', true);

-- Asignar Roles
INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (1, 1);
INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (2, 2);
INSERT INTO usuario_roles (usuario_id, rol_id) VALUES (3, 3);
