package com.tailorcv.resume;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tailorcv.resume.dto.CanonicalResume;
import com.tailorcv.resume.entity.TailoringRunEntity;
import com.tailorcv.resume.exception.ApiException;
import com.tailorcv.resume.repository.TailoringRunRepository;
import com.tailorcv.resume.service.ResumeRenderingService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResumeRenderingServiceTest {
    TailoringRunRepository runs;
    ResumeRenderingService rendering;
    ObjectMapper mapper;

    @BeforeEach void setUp() {
        runs = mock(TailoringRunRepository.class);
        mapper = new ObjectMapper();
        var resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        var engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        rendering = new ResumeRenderingService(runs, mapper, engine);
    }

    @Test void rendersEscapedHtmlAndReadablePdfFromTheSameTemplate() throws Exception {
        UUID id = UUID.randomUUID();
        var resume = new CanonicalResume("Jane <Doe>",
                new CanonicalResume.Contact("jane@example.com", "", "Brisbane"),
                "Builds reliable services.", List.of("Python", "Java"),
                List.of(new CanonicalResume.Experience("Example Co", "Software Intern", "2025-01", "2025-03",
                        List.of("Developed REST APIs."))),
                List.of(new CanonicalResume.Project("Sample Service", "University Project",
                        List.of("Python", "FastAPI"), "2025-04", "2025-06",
                        List.of("Built a fictional API."))),
                List.of(new CanonicalResume.Education("Example University", "Brisbane",
                        "Bachelor of Software Engineering", "2023-02", "2026-12",
                        List.of("Fictional scholarship recipient."))),
                List.of(new CanonicalResume.Certification("Example Cloud Certificate", "2025-07")));
        var run = new TailoringRunEntity();
        run.id = id;
        run.status = TailoringRunEntity.Status.SUCCEEDED;
        run.tailoredJson = mapper.valueToTree(resume);
        when(runs.findById(id)).thenReturn(Optional.of(run));

        String html = rendering.renderHtml(id);
        assertTrue(html.contains("Jane &lt;Doe&gt;"));
        assertFalse(html.contains("Jane <Doe>"));
        assertTrue(html.contains("Developed REST APIs."));
        assertTrue(html.contains("Sample Service"));
        assertTrue(html.contains("Example Cloud Certificate"));
        assertTrue(html.contains("Bachelor of Software Engineering"));

        byte[] pdf = rendering.renderPdf(id);
        assertArrayEquals("%PDF".getBytes(), java.util.Arrays.copyOf(pdf, 4));
        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Jane <Doe>"));
            assertTrue(text.contains("Software Intern"));
            assertTrue(text.contains("Sample Service"));
            assertTrue(text.contains("Example Cloud Certificate"));
            assertTrue(text.contains("Bachelor of Software Engineering"));
        }
    }

    @Test void omitsEmptySections() {
        UUID id = UUID.randomUUID();
        var resume = new CanonicalResume("Jane Doe", new CanonicalResume.Contact("", "", ""),
                "", List.of(), List.of(), List.of(), List.of());
        var run = new TailoringRunEntity();
        run.status = TailoringRunEntity.Status.SUCCEEDED;
        run.tailoredJson = mapper.valueToTree(resume);
        when(runs.findById(id)).thenReturn(Optional.of(run));

        String html = rendering.renderHtml(id);
        assertFalse(html.contains("Professional summary"));
        assertFalse(html.contains("<h2>Skills</h2>"));
        assertFalse(html.contains("<h2>Experience</h2>"));
    }

    @Test void rejectsUnknownAndIncompleteRuns() {
        UUID missing = UUID.randomUUID();
        when(runs.findById(missing)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ApiException.class, () -> rendering.renderHtml(missing)).status().value());

        UUID pending = UUID.randomUUID();
        var run = new TailoringRunEntity();
        run.status = TailoringRunEntity.Status.PENDING;
        when(runs.findById(pending)).thenReturn(Optional.of(run));
        assertEquals(409, assertThrows(ApiException.class, () -> rendering.renderHtml(pending)).status().value());
    }
}
