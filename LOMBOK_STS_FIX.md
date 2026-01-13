# Fixing Lombok in STS/Eclipse

## Problem
You see errors like `log cannot be resolved` in files with `@Slf4j` annotation.

## Root Cause
Lombok generates code at compile time, but STS/Eclipse needs the Lombok plugin to understand these annotations.

## Solution

### Step 1: Install Lombok in STS

**Option A: Automatic Installation**
1. Download: https://projectlombok.org/downloads/lombok.jar
2. Double-click `lombok.jar`
3. It will auto-detect STS installation
4. Click `Install/Update`
5. **Restart STS**

**Option B: Manual Installation**
1. Download `lombok.jar`
2. Copy to STS installation folder (e.g., `C:\sts-4.30.0.RELEASE`)
3. Edit `SpringToolSuite4.ini` and add **before** `-vmargs`:
   ```
   -javaagent:lombok.jar
   ```
4. **Restart STS**

### Step 2: Enable Annotation Processing

1. Right-click project → `Properties`
2. Go to `Java Compiler → Annotation Processing`
3. Check ✅ `Enable annotation processing`
4. Check ✅ `Enable processing in editor`
5. Click `Apply and Close`

### Step 3: Clean and Rebuild

1. `Project → Clean...`
2. Select your project
3. Click `Clean`
4. Wait for rebuild

### Step 4: Verify

Open any file with `@Slf4j` - the `log` errors should be gone!

## Alternative: Remove Lombok (If Issues Persist)

If Lombok installation fails, we can remove it and use standard loggers:

**In each processor file, replace:**
```java
@Slf4j
@Component
public class AuthRequestProcessor implements Processor {
```

**With:**
```java
@Component
public class AuthRequestProcessor implements Processor {
    private static final Logger log = LoggerFactory.getLogger(AuthRequestProcessor.class);
```

**Add import:**
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
```

Let me know if you want me to remove Lombok and use standard loggers instead!
