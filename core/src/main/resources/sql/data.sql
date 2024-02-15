INSERT INTO qos_profiles (name, description, status, max_upstream_rate, max_downstream_rate)
VALUES ('QOS_E', 'The QOS profile E', 'ACTIVE', '{"value": 1, "unit": "Mbps"}', '{"value": 2, "unit": "Mbps"}'),
       ('QOS_S', 'The QOS profile S', 'ACTIVE', '{"value": 10, "unit": "Mbps"}', '{"value": 20, "unit": "Mbps"}'),
       ('QOS_M', 'The QOS profile M', 'ACTIVE', '{"value": 25, "unit": "Mbps"}', '{"value": 50, "unit": "Mbps"}'),
       ('QOS_L', 'The QOS profile L', 'ACTIVE', '{"value": 50, "unit": "Mbps"}', '{"value": 100, "unit": "Mbps"}');
