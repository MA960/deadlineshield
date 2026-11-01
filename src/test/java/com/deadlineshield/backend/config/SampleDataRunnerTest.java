package com.deadlineshield.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deadlineshield.backend.model.Organization;
import com.deadlineshield.backend.repository.OrganizationRepository;
import com.deadlineshield.backend.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class SampleDataRunnerTest {

    private final ProjectRepository projectRepository = Mockito.mock(ProjectRepository.class);
    private final OrganizationRepository organizationRepository = Mockito.mock(OrganizationRepository.class);
    private final SampleDataRunner runner = new SampleDataRunner(projectRepository, organizationRepository);

    @Test
    void seedsProjectsAndMilestonesWhenTheProjectTableIsEmpty() throws Exception {
        when(projectRepository.count()).thenReturn(0L);
        ArgumentCaptor<Organization> organizationCaptor = ArgumentCaptor.forClass(Organization.class);

        runner.run();

        verify(organizationRepository).save(organizationCaptor.capture());
        Organization organization = organizationCaptor.getValue();
        assertEquals(2, organization.getProjects().size());
        assertEquals(4, organization.getProjects().stream()
                .mapToInt(project -> project.getMilestones().size())
                .sum());
        organization.getProjects().forEach(project -> {
            assertEquals(organization, project.getOrganization());
            project.getMilestones().forEach(milestone -> {
                assertEquals(project, milestone.getProject());
                assertNotNull(milestone.getToken());
            });
        });
    }

    @Test
    void doesNotSeedWhenProjectsAlreadyExist() throws Exception {
        when(projectRepository.count()).thenReturn(1L);

        runner.run();

        verify(organizationRepository, never()).save(Mockito.any());
    }
}
