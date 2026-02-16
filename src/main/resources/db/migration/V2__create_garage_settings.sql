CREATE TABLE IF NOT EXISTS garage_settings (
                                               id BIGSERIAL PRIMARY KEY,
                                               garage_name           VARCHAR(120) NOT NULL,
    address               VARCHAR(255) NOT NULL,
    phone                 VARCHAR(40),
    kvk_number            VARCHAR(40),
    logo_url              VARCHAR(255),

    template_confirmation TEXT NOT NULL,
    template_ready        TEXT NOT NULL,

    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
    );

-- 1 row seed (id=1)
INSERT INTO garage_settings (
    id, garage_name, address, phone, kvk_number, logo_url,
    template_confirmation, template_ready
) VALUES (
             1,
             'Tay Performance',
             'Vul adres in',
             '',
             '',
             '',
             'Hoi {{customerName}}, je afspraak bij {{garageName}} staat gepland op {{date}}. Adres: {{address}}. Prijs: €{{price}}.',
             'Hoi {{customerName}}, je auto is klaar. Je kan langskomen bij {{garageName}} op {{address}}.'
         )
    ON CONFLICT (id) DO NOTHING;
