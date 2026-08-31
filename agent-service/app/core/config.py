from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import List


class Settings(BaseSettings):
    PROJECT_NAME: str = "Autonomous Code Review - Agent Service"
    VERSION: str = "0.1.0"
    API_V1_STR: str = "/api/v1"
    PORT: int = 8000
    HOST: str = "0.0.0.0"
    CORS_ORIGINS: List[str] = [
        "http://localhost:3000",
        "http://localhost:5173",
        "http://localhost:8080",
    ]
    LLM_PROVIDER: str = "gemini"  # Default: gemini, options: ollama, openai, gemini
    ENVIRONMENT: str = "development"

    model_config = SettingsConfigDict(case_sensitive=True, env_file=".env")



settings = Settings()
