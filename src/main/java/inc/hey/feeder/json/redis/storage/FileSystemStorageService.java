package inc.hey.feeder.json.redis.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.autoconfigure.vectorstore.redis.RedisVectorStoreProperties;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.vectorstore.RedisVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

@Service
public class FileSystemStorageService implements StorageService {

    private final Logger logger = LoggerFactory.getLogger(FileSystemStorageService.class);
    private final String[] KEYS = { "area", "tema", "subtema", "nomsubtema", "nommaterias", "contenido" };

    private Path rootLocation = null;
    private RedisVectorStore vectorStore = null;
    private RedisVectorStoreProperties properties = null;

    public FileSystemStorageService(RedisVectorStore vectorStore,
                                    RedisVectorStoreProperties properties) {
        this.vectorStore = vectorStore;
        this.properties = properties;
    }

    @Autowired
    public FileSystemStorageService(StorageProperties properties) {

        if(properties.getLocation().trim().length() == 0){
            throw new StorageException("La ubicacion de carga del archivo no puede estar vacía.");
        }

        this.rootLocation = Paths.get(properties.getLocation());
    }

    @Override
    public void store(MultipartFile file) {
        try {
            if (file.isEmpty()) {
                throw new StorageException("Falla al guardar un archivo vacio.");
            }
            Path destinationFile = this.rootLocation.resolve(
                            Paths.get(file.getOriginalFilename()))
                    .normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath())) {
                // This is a security check
                throw new StorageException(
                        "No se puede guardar un archivo fuera del directorio actual.");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile,
                        StandardCopyOption.REPLACE_EXISTING);
            }

            logger.info("Creando los Embeddings...");
            Resource ff = file.getResource();
            JsonReader loader = new JsonReader(ff, KEYS);
            logger.info("Archivo leido... " + loader.toString().length());
            vectorStore.add(loader.get());
            logger.info("Archivo cargado... ");
            logger.info("Embeddings creados.");
        } catch (IOException e) {
            throw new StorageException("Falla al guardar el archivo.", e);
        }
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(this.rootLocation::relativize);
        }
        catch (IOException e) {
            throw new StorageException("Falla al leer los archivos almacenados", e);
        }

    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new StorageFileNotFoundException(
                        "No se pudo leer el archivo: " + filename);

            }
        }
        catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("No se pudo leer el archivo: " + filename, e);
        }
    }

    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        }
        catch (IOException e) {
            throw new StorageException("No se pudo inicializar la carpeta contenedora", e);
        }
    }
}
