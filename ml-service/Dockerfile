FROM python:3.10-slim
WORKDIR /app

# Install dependencies carefully to leverage Docker cache
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy source
COPY . .

# Expose the API port
EXPOSE 5001

# Command to run (using Waitress in production rather than raw flask app.run if possible, but app.py runs waitress/flask)
CMD ["python", "app.py"]
