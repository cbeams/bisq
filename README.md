# Bisq

## Build

./gradlew build

## Run

./fx/build/app/bin/bisq-fx

## Import into IDEA

### Configure IDEA properties

Before importing the project, open IDEA, go to `Help->Edit Custom Properties...` and add the following line:

    idea.max.intellisense.filesize=12500

This ensures that IDEA will be able to handle Bisq's very large generated `PB.java` Protobuf sources. Without changing this setting, you will get compilation errors in IDEA wherever these generated types are used.

Close and restart IDEA for the new setting to take effect.

### Enable annotation processing

Go to `Preferences->Build, Execution, Deployment->Annotation Processors` and check `Enable annotation processing`.

This allows Lombok annotations to work and avoids compilation errors.

### Import the project

Go to `File->Open...` and choose this `build.gradle` file. Accept all defaults and click OK. The project should import without errors.
