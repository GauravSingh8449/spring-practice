package com.parctice.spring.demo.service;

import com.parctice.spring.demo.model.Document;
import com.parctice.spring.demo.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class DocumentService {

    private final String uploadDir = new File("uploads").getAbsolutePath() + File.separator;

    @Autowired
    private DocumentRepository documentRepository;

    public List<Document> getUserDocuments(Long userId) {
        return documentRepository.findByUserId(userId);
    }

    public String saveUserDocument(MultipartFile file, Long userId) throws IOException {
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path path = Paths.get(uploadDir + filename);
        Files.write(path, file.getBytes());

        Document doc = new Document();
        doc.setUserId(userId);
        doc.setFileName(file.getOriginalFilename());
        doc.setFilePath(filename);
        documentRepository.save(doc);

        return filename;
    }

    public Optional<Document> getById(Long id) {
        return documentRepository.findById(id);
    }

    public Resource getDocumentAsResource(Long id) {
        Optional<Document> docOpt = getById(id);
        if (docOpt.isPresent()) {
            String filename = docOpt.get().getFilePath();
            Path path = Paths.get(uploadDir).resolve(filename).normalize();
            File file = path.toFile();
            if(file.exists()) {
                return new FileSystemResource(file);
            }
        }
        return null;
    }

    // ================= Delete Document =================
    public boolean deleteDocument(Long id) {
        Optional<Document> docOpt = getById(id);
        if (docOpt.isPresent()) {
            Document doc = docOpt.get();
            Path path = Paths.get(uploadDir).resolve(doc.getFilePath()).normalize();
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            documentRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
