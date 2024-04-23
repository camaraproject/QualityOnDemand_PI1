INSERT INTO qos_profiles (name, description, status, max_upstream_rate, max_downstream_rate, min_duration, max_duration)
VALUES ('QOS_E', 'The QOS profile E', 'ACTIVE', '{"value": 1, "unit": "Mbps"}', '{"value": 2, "unit": "Mbps"}', '{"value": 10, "unit": "Seconds"}', '{"value": 30, "unit": "Seconds"}'),
       ('QOS_S', 'The QOS profile S', 'ACTIVE', '{"value": 10, "unit": "Mbps"}', '{"value": 20, "unit": "Mbps"}', '{"value": 10, "unit": "Seconds"}', '{"value": 5, "unit": "Minutes"}'),
       ('QOS_M', 'The QOS profile M', 'ACTIVE', '{"value": 25, "unit": "Mbps"}', '{"value": 50, "unit": "Mbps"}', '{"value": 10, "unit": "Seconds"}', '{"value": 5, "unit": "Hours"}'),
       ('QOS_L', 'The QOS profile L', 'ACTIVE', '{"value": 50, "unit": "Mbps"}', '{"value": 100, "unit": "Mbps"}', '{"value": 10, "unit": "Seconds"}', '{"value": 12, "unit": "Hours"}');
