package com.deadlineshield.backend.config;

import com.deadlineshield.backend.model.Milestone;
import com.deadlineshield.backend.model.Organization;
import com.deadlineshield.backend.model.Project;
import com.deadlineshield.backend.repository.OrganizationRepository;
import com.deadlineshield.backend.repository.ProjectRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SampleDataRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SampleDataRunner.class);

    private final ProjectRepository projectRepository;
    private final OrganizationRepository organizationRepository;

    public SampleDataRunner(
            ProjectRepository projectRepository,
            OrganizationRepository organizationRepository) {
        this.projectRepository = projectRepository;
        this.organizationRepository = organizationRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (projectRepository.count() > 0) {
            logger.info("Skipping sample data initialization because projects already exist");
            return;
        }

        Instant now = Instant.now();
        Organization organization = new Organization();
        organization.setName("DeadlineShield Demo Agency");

        Project website = createProject(
                organization,
                "Website redesign",
                "Refresh the client website and prepare it for launch.",
                now.plus(30, ChronoUnit.DAYS),
                List.of(
                        createMilestone("Approve homepage design", now.plus(10, ChronoUnit.DAYS)),
                        createMilestone("Complete responsive implementation", now.plus(24, ChronoUnit.DAYS))));
        Project brand = createProject(
                organization,
                "Brand identity package",
                "Create and deliver a complete visual identity system.",
                now.plus(45, ChronoUnit.DAYS),
                List.of(
                        createMilestone("Review initial concepts", now.plus(14, ChronoUnit.DAYS)),
                        createMilestone("Approve final brand assets", now.plus(38, ChronoUnit.DAYS))));

        organization.setProjects(List.of(website, brand));
        organizationRepository.save(organization);
        logger.info("Seeded sample DeadlineShield projects and milestones");
    }

    private Project createProject(
            Organization organization,
            String name,
            String description,
            Instant adjustedTargetDate,
            List<Milestone> milestones) {
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        project.setAdjustedTargetDate(adjustedTargetDate);
        project.setOrganization(organization);
        project.setMilestones(milestones);
        milestones.forEach(milestone -> milestone.setProject(project));
        return project;
    }

    private Milestone createMilestone(String name, Instant dueAt) {
        Milestone milestone = new Milestone();
        milestone.setName(name);
        milestone.setDueAt(dueAt);
        return milestone;
    }
}
