import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

abstract class ModifyClassesTask: DefaultTask() {

    // This property will be set to all Jar files available in scope
    @get:InputFiles
    abstract val allJars: ListProperty<RegularFile>

    // Gradle will set this property with all class directories that available in scope
    @get:InputFiles
    abstract val allDirectories: ListProperty<Directory>

    // Task will put all classes from directories and jars after optional modification into single jar
    @get:OutputFile
    abstract val output: RegularFileProperty

    @Internal
    val jarPaths = mutableSetOf<String>()

    @TaskAction
    fun doModify() {
//        val pool = ClassPool(ClassPool.getDefault()) (may change to asm.)

        val jarOutput = JarOutputStream(BufferedOutputStream(FileOutputStream(output.get().asFile)))
        // we just copying classes from jar files without modification
        allJars.get().forEach { file ->
            if (isCodeSource(file.asFile.absolutePath)) {
                println("handling " + file.asFile.absolutePath)
                val jarFile = JarFile(file.asFile)
                jarFile.entries().iterator().forEach { jarEntry ->
                    println("Adding from jar ${jarEntry.name}")
                    jarOutput.writeEntity(jarEntry.name, jarFile.getInputStream(jarEntry))
                }
                jarFile.close()
            }
        }
        // Iterating through class files from directories
        // Looking for SomeSource.class to add generated interface and instrument with additional output in
        // toString methods (in our case it's just System.out)
        allDirectories.get().forEach { directory ->
            println("handling " + directory.asFile.absolutePath)
            directory.asFile.walk().forEach { file ->
                if (file.isFile) {
                    if (file.name.endsWith("MainActivity.class")) {
                        println("Found ${file.name}")
                        val mCr = ClassReader(file.inputStream())
                        val mCw = ClassWriter(mCr, 0)
                        val mCv = AsmClassVisitor(Opcodes.ASM5, mCw)
                        mCr.accept(mCv, ClassReader.EXPAND_FRAMES)
                        val resultByteArray = mCw.toByteArray()
                        jarOutput.writeEntity("", resultByteArray)
//                        val interfaceClass = pool.makeInterface("com.example.android.recipes.sample.SomeInterface");
//                        println("Adding $interfaceClass")
//                        jarOutput.writeEntity("com/example/android/recipes/sample/SomeInterface.class", interfaceClass.toBytecode())
//                        val ctClass = file.inputStream().use {
//                            pool.makeClass(it);
//                        }
//                        ctClass.addInterface(interfaceClass)

//                        val m = ctClass.getDeclaredMethod("toString");
//                        if (m != null) {
//                            // injecting additional code that will be located at the beginning of toString method
//                            m.insertBefore("{ System.out.println(\"Some Extensive Tracing\"); }");
//
//                            val relativePath = directory.asFile.toURI().relativize(file.toURI()).getPath()
//                            // Writing changed class to output jar
//                            jarOutput.writeEntity(relativePath.replace(File.separatorChar, '/'), ctClass.toBytecode())
//                        }
                    } else {
                        // if class is not SomeSource.class - just copy it to output without modification
                        val relativePath = directory.asFile.toURI().relativize(file.toURI()).getPath()
                        println("Adding from directory ${relativePath.replace(File.separatorChar, '/')}")
                        jarOutput.writeEntity(relativePath.replace(File.separatorChar, '/'), file.inputStream())
                    }
                }
            }
        }
        jarOutput.close()
    }

    // writeEntity methods check if the file has name that already exists in output jar
    private fun JarOutputStream.writeEntity(name: String, inputStream: InputStream) {
        // check for duplication name first
        if (jarPaths.contains(name)) {
            printDuplicatedMessage(name)
        } else {
            putNextEntry(JarEntry(name))
            inputStream.copyTo(this)
            closeEntry()
            jarPaths.add(name)
        }
    }

    private fun JarOutputStream.writeEntity(relativePath: String, byteArray: ByteArray) {
        // check for duplication name first
        if (jarPaths.contains(relativePath)) {
            printDuplicatedMessage(relativePath)
        } else {
            putNextEntry(JarEntry(relativePath))
            write(byteArray)
            closeEntry()
            jarPaths.add(relativePath)
        }
    }

    private fun printDuplicatedMessage(name: String) =
        println("Cannot add ${name}, because output Jar already has file with the same name.")

    /**
     * 筛选 .class 源码
     *
     * @param codeSource class 文件路径
     */
    private fun isCodeSource(codeSource: String): Boolean {
        return codeSource.endsWith(".class") &&
                !codeSource.contains("R\$") &&
                !codeSource.contains("R.class") &&
                !codeSource.contains("R2\$") &&
                !codeSource.contains("R2.class") &&
                !codeSource.contains("BuildConfig.class")
    }

    /// 类访问
    private class AsmClassVisitor(api: Int, cv: ClassVisitor): ClassVisitor(api, cv) {
        override fun visit(
            version: Int,
            access: Int,
            name: String?,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?
        ) {
            super.visit(version, access, name, signature, superName, interfaces)
        }

        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }
    }

    /// 方法访问
    private class AsmMethodVisitor(api: Int, mv: MethodVisitor): MethodVisitor(api, mv) {
        override fun visitInsn(opcode: Int) {
            if (opcode in Opcodes.IRETURN..Opcodes.RETURN) {
                mv.visitMethodInsn(Opcodes.INVOKESTATIC, "", "", "", false)
            }
            super.visitInsn(opcode)
        }

        override fun visitMaxs(maxStack: Int, maxLocals: Int) {
            super.visitMaxs(maxStack, maxLocals)
        }
    }
}