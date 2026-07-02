#!/usr/bin/env bash
# Exporta las especificaciones OpenAPI 3.0 a docs/api/ una vez que el stack
# está arriba (docker compose up --build). Requiere que ambos servicios
# estén healthy.
#
# Uso:  bash scripts/exportar-openapi.sh
set -euo pipefail

OUT_DIR="$(dirname "$0")/../docs/api"
mkdir -p "$OUT_DIR"

echo "Exportando OpenAPI de svc-catalogo..."
docker compose exec -T svc-catalogo curl -s http://localhost:8000/api/v1/openapi.json > "$OUT_DIR/catalogo.json"

echo "Exportando OpenAPI de svc-pedidos..."
docker compose exec -T svc-pedidos curl -s http://localhost:8000/api/v1/openapi.json > "$OUT_DIR/pedidos.json"

echo "Listo -> $OUT_DIR/catalogo.json y $OUT_DIR/pedidos.json"
echo "(Opcional) conviértelos a YAML con: pip install pyyaml && python3 -c \"import json,yaml,sys; yaml.dump(json.load(open(sys.argv[1])), open(sys.argv[2],'w'), allow_unicode=True)\" catalogo.json catalogo.yaml"
