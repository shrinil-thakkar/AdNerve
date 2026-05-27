UPDATE screens
SET device_token = upper(substring(md5(random()::text || id::text), 1, 8));

ALTER TABLE screens
    ALTER COLUMN device_token TYPE VARCHAR(16);
