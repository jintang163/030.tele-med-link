CREATE DATABASE IF NOT EXISTS tele_med_link DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE tele_med_link;

CREATE TABLE t_hospital (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    address VARCHAR(512),
    phone VARCHAR(20),
    level VARCHAR(32),
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_campus (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hospital_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    address VARCHAR(512),
    phone VARCHAR(20),
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (hospital_id) REFERENCES t_hospital(id),
    INDEX idx_hospital_id (hospital_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(256) NOT NULL,
    real_name VARCHAR(64),
    phone VARCHAR(20) UNIQUE,
    role VARCHAR(20) NOT NULL,
    avatar_url VARCHAR(512),
    open_id VARCHAR(128) UNIQUE,
    hospital_id BIGINT,
    department VARCHAR(64),
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_doctor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    title VARCHAR(64),
    specialty VARCHAR(128),
    department VARCHAR(64),
    hospital_id BIGINT,
    campus_id BIGINT,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_patient (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    name VARCHAR(64),
    gender INT,
    age INT,
    id_card VARCHAR(256),
    medical_card_no VARCHAR(64),
    hospital_id BIGINT,
    campus_id BIGINT,
    open_id VARCHAR(128) UNIQUE,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES t_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hospital_id BIGINT NOT NULL,
    campus_id BIGINT,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(256),
    sort_order INT DEFAULT 0,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (hospital_id) REFERENCES t_hospital(id),
    INDEX idx_hospital_id (hospital_id),
    INDEX idx_campus_id (campus_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_consultation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    consultation_no VARCHAR(64) NOT NULL UNIQUE,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT,
    hospital_id BIGINT,
    campus_id BIGINT,
    status INT NOT NULL DEFAULT 0,
    type INT NOT NULL DEFAULT 0,
    appointment_id BIGINT,
    room_id VARCHAR(64),
    start_time DATETIME,
    end_time DATETIME,
    duration INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES t_patient(id),
    FOREIGN KEY (doctor_id) REFERENCES t_doctor(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_appointment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    hospital_id BIGINT,
    appointment_date DATE NOT NULL,
    time_slot INT NOT NULL,
    status INT NOT NULL DEFAULT 0,
    description VARCHAR(500),
    consultation_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES t_patient(id),
    FOREIGN KEY (doctor_id) REFERENCES t_doctor(id),
    UNIQUE KEY uk_doctor_date_slot (doctor_id, appointment_date, time_slot)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    consultation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    sender_role VARCHAR(20) NOT NULL,
    content VARCHAR(2000),
    content_type INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (consultation_id) REFERENCES t_consultation(id),
    INDEX idx_consultation_id (consultation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_consultation_conclusion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    consultation_id BIGINT NOT NULL UNIQUE,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    content TEXT,
    file_url VARCHAR(512),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (consultation_id) REFERENCES t_consultation(id),
    FOREIGN KEY (doctor_id) REFERENCES t_doctor(id),
    FOREIGN KEY (patient_id) REFERENCES t_patient(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO t_hospital (name, address, phone, level) VALUES
('市中心医院', '北京市朝阳区健康路100号', '010-88888888', '三级甲等'),
('第一人民医院', '北京市海淀区学院路50号', '010-66666666', '三级乙等');

INSERT INTO t_campus (hospital_id, name, address, phone) VALUES
(1, '东院区', '北京市朝阳区健康路100号', '010-88888001'),
(1, '西院区', '北京市西城区康复街20号', '010-88888002'),
(2, '主院区', '北京市海淀区学院路50号', '010-66666001');

INSERT INTO t_department (hospital_id, campus_id, name, description, sort_order) VALUES
(1, 1, '心血管内科', '心血管疾病诊治', 1),
(1, 1, '呼吸内科', '呼吸系统疾病', 2),
(1, 1, '消化内科', '消化系统疾病', 3),
(1, 2, '普外科', '普通外科手术', 1),
(1, 2, '骨科', '骨科疾病诊治', 2),
(2, 3, '内科', '综合内科', 1),
(2, 3, '外科', '综合外科', 2);

INSERT INTO t_user (username, password, real_name, phone, role, hospital_id, department) VALUES
('doctor1', '123456', '张医生', '13800001111', 'DOCTOR', 1, '内科'),
('doctor2', '123456', '李医生', '13800002222', 'DOCTOR', 1, '外科'),
('doctor3', '123456', '王医生', '13800003333', 'DOCTOR', 2, '内科');

INSERT INTO t_doctor (user_id, title, specialty, department, hospital_id, campus_id) VALUES
(1, '主任医师', '心血管内科', '内科', 1, 1),
(2, '副主任医师', '普外科', '外科', 1, 2),
(3, '主治医师', '呼吸内科', '内科', 2, 3);

INSERT INTO t_user (username, password, real_name, role) VALUES
('admin', 'admin123', '系统管理员', 'ADMIN');

INSERT INTO t_user (username, password, real_name, phone, role, open_id, hospital_id) VALUES
('patient1', '123456', '王患者', '13900001111', 'PATIENT', 'oTestOpenId001', 1),
('patient2', '123456', '刘患者', '13900002222', 'PATIENT', 'oTestOpenId002', 2);

INSERT INTO t_patient (user_id, name, gender, age, medical_card_no, hospital_id, campus_id, open_id) VALUES
(4, '王患者', 1, 45, 'MC20240001', 1, 1, 'oTestOpenId001'),
(5, '刘患者', 0, 32, 'MC20240002', 2, 3, 'oTestOpenId002');

CREATE TABLE t_video_recording (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    consultation_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    status INT NOT NULL DEFAULT 0,
    segment_duration INT DEFAULT 300,
    total_segments INT DEFAULT 0,
    total_duration INT DEFAULT 0,
    encryption_key VARCHAR(512),
    encryption_iv VARCHAR(128),
    hls_playlist_url VARCHAR(512),
    hls_bucket VARCHAR(128),
    hls_object_name VARCHAR(512),
    doctor_authorized TINYINT DEFAULT 0,
    patient_authorized TINYINT DEFAULT 0,
    watermark_text VARCHAR(256),
    expire_time DATETIME NOT NULL,
    start_time DATETIME,
    end_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (consultation_id) REFERENCES t_consultation(id),
    FOREIGN KEY (doctor_id) REFERENCES t_doctor(id),
    FOREIGN KEY (patient_id) REFERENCES t_patient(id),
    INDEX idx_consultation_id (consultation_id),
    INDEX idx_status (status),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_video_segment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recording_id BIGINT NOT NULL,
    consultation_id BIGINT NOT NULL,
    segment_index INT NOT NULL,
    file_name VARCHAR(256) NOT NULL,
    bucket_name VARCHAR(128) NOT NULL,
    object_name VARCHAR(512) NOT NULL,
    file_size BIGINT DEFAULT 0,
    duration INT DEFAULT 0,
    encryption_iv VARCHAR(128),
    checksum VARCHAR(128),
    upload_status INT DEFAULT 0,
    upload_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recording_id) REFERENCES t_video_recording(id),
    FOREIGN KEY (consultation_id) REFERENCES t_consultation(id),
    INDEX idx_recording_id (recording_id),
    INDEX idx_upload_status (upload_status),
    UNIQUE KEY uk_recording_segment (recording_id, segment_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE t_video_playback_auth (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recording_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    user_role VARCHAR(20) NOT NULL,
    auth_token VARCHAR(512) NOT NULL,
    expire_time DATETIME NOT NULL,
    play_count INT DEFAULT 0,
    last_play_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (recording_id) REFERENCES t_video_recording(id),
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    INDEX idx_recording_id (recording_id),
    INDEX idx_auth_token (auth_token),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
