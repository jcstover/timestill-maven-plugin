package nl.nilling.maven.plugin.timestill.mojo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import org.junit.Assert;
import org.junit.Test;

/**
 * test for the timestill mojo.
 */
public class TimestillMojoTest {

    private TimestillMojo subject = new TimestillMojo();

    @Test
    public void testTimestill() throws Exception {
        createTestJarFile(FileTime.from(Instant.ofEpochSecond(10_000)));
        final String testJar = FileSystems.getDefault().getPath("target/testjar.jar").toFile().getAbsolutePath();
        subject.setTime("201701011200");
        subject.setArtifact(testJar);
        subject.execute();
        final String firstChecksum = calculateChecksum();
        Files.deleteIfExists(FileSystems.getDefault().getPath("target/testjar.jar"));
        createTestJarFile(FileTime.from(Instant.ofEpochSecond(100_000)));
        subject.setTime("201701011200");
        subject.setArtifact(testJar);
        subject.execute();
        final String secondChecksum = calculateChecksum();
        Assert.assertEquals(firstChecksum, secondChecksum);
    }

    private void createTestJarFile(final FileTime fileTime) throws Exception {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "nl.nilling.FooBar");
        try (final JarOutputStream nieuwArchiefStream = new JarOutputStream(new FileOutputStream("target/testjar.jar"), manifest)) {
            final Path jarContentPath = FileSystems.getDefault().getPath("target/test-classes/jarcontent");
            processFiles(jarContentPath.toFile(), nieuwArchiefStream, fileTime);
        }
    }

    private String calculateChecksum() throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        try (FileInputStream fis = new FileInputStream("target/testjar.jar");
             DigestInputStream dis = new DigestInputStream(fis, md5)) {
            byte[] buffer = new byte[2048];
            while (dis.read(buffer) > 0) {
                // just reading
            }
        }
        return md5.toString();
    }

    private void processFiles(final File root, final JarOutputStream nieuwArchiefStream, final FileTime fileTime) throws IOException {
        for (final File file : root.listFiles()) {
            if (file.isDirectory()) {
                processFiles(file, nieuwArchiefStream, fileTime);
            }
            addEntry(file.toPath(), fileTime, nieuwArchiefStream);
        }

    }

    private void addEntry(final Path file, final FileTime fileTime, final JarOutputStream nieuwArchiefStream) throws IOException {
        final File archiveFile = file.toFile();
        JarEntry newEntry = new JarEntry(archiveFile.getAbsolutePath().substring(1));
        fixateDateAndTimeOfEntry(newEntry, fileTime);
        nieuwArchiefStream.putNextEntry(newEntry);
        if (archiveFile.isFile()) {
            packFile(nieuwArchiefStream, archiveFile);
        }
        nieuwArchiefStream.closeEntry();
    }

    private void fixateDateAndTimeOfEntry(final ZipEntry entry, final FileTime time) {
        entry.setCreationTime(time);
        entry.setLastAccessTime(time);
        entry.setLastModifiedTime(time);
    }

    private void packFile(final JarOutputStream nieuwArchiefStream, final File archiefBestand) throws IOException {
        try (final FileInputStream fis = new FileInputStream(archiefBestand);
             final BufferedInputStream bis = new BufferedInputStream(fis)) {
            int len;
            byte[] buffer = new byte[2048];
            while ((len = bis.read(buffer)) > 0) {
                nieuwArchiefStream.write(buffer, 0, len);
            }
        }
    }
}