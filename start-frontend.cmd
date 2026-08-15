@echo off
setlocal

set "REACT_APP_API_URL=http://localhost:8080"
cd /d "%~dp0frontend"
call npm.cmd start
