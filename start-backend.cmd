@echo off
setlocal

if exist "%~dp0backend\local-dev.cmd" call "%~dp0backend\local-dev.cmd"

if not defined DB_URL set "DB_URL=jdbc:mysql://localhost:3306/skillforge"
if not defined DB_USERNAME set "DB_USERNAME=root"
if not defined DB_PASSWORD set "DB_PASSWORD=REPLACE_WITH_MYSQL_PASSWORD"
if not defined FRONTEND_URL set "FRONTEND_URL=http://localhost:3000"
if not defined JWT_SECRET set "JWT_SECRET=REPLACE_WITH_A_BASE64_SECRET_AT_LEAST_32_BYTES_LONG"
if not defined JWT_EXPIRATION_MS set "JWT_EXPIRATION_MS=86400000"
if not defined GEMINI_API_KEY set "GEMINI_API_KEY=REPLACE_WITH_GEMINI_API_KEY"
if not defined GEMINI_API_KEYS set "GEMINI_API_KEYS=REPLACE_WITH_OPTIONAL_COMMA_SEPARATED_KEYS"
if not defined GEMINI_API_URL set "GEMINI_API_URL=https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
if not defined GEMINI_FALLBACK_ENABLED set "GEMINI_FALLBACK_ENABLED=true"
if not defined OPENROUTER_API_KEY set "OPENROUTER_API_KEY=REPLACE_WITH_OPENROUTER_KEY"
if not defined OPENROUTER_API_URL set "OPENROUTER_API_URL=https://openrouter.ai/api/v1/chat/completions"
if not defined OPENROUTER_MODEL set "OPENROUTER_MODEL=openrouter/free"

call "%~dp0tools\apache-maven-3.9.11\bin\mvn.cmd" -f "%~dp0backend\pom.xml" spring-boot:run
