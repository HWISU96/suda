# S14P31A404

## 필수 버전

- Java 21

## 백엔드 점검

```bash
cd backend
./gradlew spotlessCheck --no-daemon
./gradlew build -x test --no-daemon
```

## 모바일 점검

```bash
cd mobile
./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug --no-daemon
```
