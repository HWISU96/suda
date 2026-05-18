FROM python:3.12-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

COPY ai-server/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY ai-server/app ./app
COPY src/sign2text_tcn/model.py ./src/sign2text_tcn/model.py
COPY models/tcn_mobile/train_config_v6.json ./models/tcn_mobile/train_config_v6.json
COPY models/tcn_mobile/label_map_v6.json ./models/tcn_mobile/label_map_v6.json

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
