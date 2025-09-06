package com.parctice.spring.demo.controller;

import com.parctice.spring.demo.model.Document;
import com.parctice.spring.demo.model.User;
import com.parctice.spring.demo.service.DocumentService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    // ================= Show Documents Page =================
    @GetMapping
    public String showDocuments(HttpSession session, Model model) {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        List<Document> docs = documentService.getUserDocuments(user.getId());
        model.addAttribute("user", user);
        model.addAttribute("documents", docs);

        // Messages from session
        if (session.getAttribute("successMessage") != null) {
            model.addAttribute("successMessage", session.getAttribute("successMessage"));
            session.removeAttribute("successMessage");
        }
        if (session.getAttribute("errorMessage") != null) {
            model.addAttribute("errorMessage", session.getAttribute("errorMessage"));
            session.removeAttribute("errorMessage");
        }

        return "documents";
    }

    // ================= Handle Document Upload =================
    @PostMapping("/upload")
    public String uploadDocument(@RequestParam("file") MultipartFile file,
                                 HttpSession session) throws IOException {
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) return "redirect:/login";

        if (file == null || file.isEmpty()) {
            session.setAttribute("errorMessage", "Please select a file to upload.");
        } else {
            documentService.saveUserDocument(file, user.getId());
            session.setAttribute("successMessage", "Document uploaded successfully!");
        }

        return "redirect:/documents"; // Redirect avoids resubmission
    }

    // ================= Handle Document Download =================
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {
        Optional<Document> docOpt = documentService.getById(id);
        if (docOpt.isEmpty()) return ResponseEntity.notFound().build();

        Document doc = docOpt.get();
        Resource resource = documentService.getDocumentAsResource(id);
        if (resource == null || !resource.exists()) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + doc.getFileName() + "\"")
                .header("Content-Type", "application/octet-stream")
                .body(resource);
    }

    // ================= Handle Document Preview =================
    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewDocument(@PathVariable Long id) {
        Optional<Document> docOpt = documentService.getById(id);
        if (docOpt.isEmpty()) return ResponseEntity.notFound().build();

        Document doc = docOpt.get();
        Resource resource = documentService.getDocumentAsResource(id);
        if (resource == null || !resource.exists()) return ResponseEntity.notFound().build();

        String contentType = "application/octet-stream";
        String fileName = doc.getFileName().toLowerCase();
        if (fileName.endsWith(".pdf")) contentType = "application/pdf";
        else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) contentType = "image/jpeg";
        else if (fileName.endsWith(".png")) contentType = "image/png";
        else if (fileName.endsWith(".gif")) contentType = "image/gif";

        return ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"" + doc.getFileName() + "\"")
                .header("Content-Type", contentType)
                .body(resource);
    }

    // ================= Handle Document Delete via AJAX =================
    @PostMapping("/delete")
    @ResponseBody
    public Map<String, Object> deleteDocumentAjax(@RequestParam("id") Long id, HttpSession session) {
        Map<String, Object> resp = new HashMap<>();
        User user = (User) session.getAttribute("loggedInUser");
        if (user == null) {
            resp.put("status", "error");
            resp.put("message", "User not logged in");
            return resp;
        }

        Optional<Document> docOpt = documentService.getById(id);
        if (docOpt.isEmpty()) {
            resp.put("status", "error");
            resp.put("message", "Document not found");
            return resp;
        }

        Document doc = docOpt.get();
        if (!doc.getUserId().equals(user.getId())) {
            resp.put("status", "error");
            resp.put("message", "You are not allowed to delete this document");
            return resp;
        }

        boolean deleted = documentService.deleteDocument(id);
        if (deleted) {
            resp.put("status", "success");
            resp.put("message", "Document deleted successfully!");
        } else {
            resp.put("status", "error");
            resp.put("message", "Failed to delete document");
        }

        return resp;
    }
}
