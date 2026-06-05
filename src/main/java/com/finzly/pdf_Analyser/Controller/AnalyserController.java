package com.finzly.pdf_Analyser.Controller;

import com.finzly.pdf_Analyser.Model.AnalysisModel;
import com.finzly.pdf_Analyser.Service.GeminiService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


@Controller
public class AnalyserController {


    private final GeminiService geminiService;

    public AnalyserController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    // Handles GET / → shows the empty form
    @GetMapping("/")
    public String home() {
        return "index"; // resolves to src/main/resources/templates/index.html
    }

    // Handles POST /analyse → called when user clicks the button
    // @RequestParam maps the form field named "pdfUrl" to the String parameter
    @PostMapping("/analyse")
    public String analyse(@RequestParam String pdfUrl, Model model) {
        AnalysisModel result = geminiService.analysePdf(pdfUrl);

        // Clean the error message — show only short readable text
        if (result.getError() != null) {
            String raw = result.getError();
            String title = "Analysis Failed";
            String message;

            if (raw.contains("403"))       message = "403 Forbidden — The server denied access to this PDF.";
            else if (raw.contains("404"))  message = "404 Not Found — The PDF URL does not exist.";
            else if (raw.contains("503"))  message = "503 Service Unavailable — The server is temporarily down.";
            else if (raw.contains("429"))  message = "429 Too Many Requests — API quota exceeded. Try again shortly.";
            else if (raw.contains("not a valid PDF")) message = "The URL did not return a PDF file. Please use a direct .pdf link.";
            else if (raw.contains("timed out") || raw.contains("timeout")) message = "Request timed out. The PDF may be too large or the server is slow.";
            else if (raw.contains("No content")) message = "No content received. The URL may be empty or unreachable.";
            else message = "Something went wrong. Please check the URL and provide valid URL.";

            model.addAttribute("errorTitle", title);
            model.addAttribute("errorMessage", message);
        }

        model.addAttribute("result", result);
        model.addAttribute("pdfUrl", pdfUrl);
        return "index";
    }
}
