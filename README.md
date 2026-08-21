# Sistema de Gestao Financeira Pessoal

Bootstrap do backend conforme a T01 da especificacao v3.2.

## Requisitos

- Java 25
- Docker Desktop ou runtime compativel

## Validacao

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.3'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

.\mvnw.cmd -v
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

O perfil `local` usa o `docker-compose.yml`. A primeira migration de negocio sera criada somente na T02.
