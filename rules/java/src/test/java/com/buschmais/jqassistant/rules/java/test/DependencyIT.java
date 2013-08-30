package com.buschmais.jqassistant.rules.java.test;

import com.buschmais.jqassistant.core.analysis.api.AnalyzerException;
import com.buschmais.jqassistant.core.analysis.test.AbstractAnalysisIT;
import com.buschmais.jqassistant.core.model.api.Result;
import com.buschmais.jqassistant.core.model.api.rule.Constraint;
import com.buschmais.jqassistant.rules.java.test.set.dependency.typebodies.FieldAnnotation;
import com.buschmais.jqassistant.rules.java.test.set.dependency.typebodies.TypeBody;
import com.buschmais.jqassistant.rules.java.test.set.dependency.typebodies.MethodAnnotation;
import com.buschmais.jqassistant.rules.java.test.set.dependency.packages.a.A;
import com.buschmais.jqassistant.rules.java.test.set.dependency.packages.b.B;
import com.buschmais.jqassistant.rules.java.test.set.dependency.types.DependentType;
import com.buschmais.jqassistant.rules.java.test.set.dependency.types.SuperType;
import com.buschmais.jqassistant.rules.java.test.set.dependency.types.TypeAnnotation;
import org.hamcrest.Matcher;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.buschmais.jqassistant.core.model.test.matcher.ResultMatcher.result;
import static com.buschmais.jqassistant.core.model.test.matcher.descriptor.ArtifactDescriptorMatcher.artifactDescriptor;
import static com.buschmais.jqassistant.core.model.test.matcher.descriptor.PackageDescriptorMatcher.packageDescriptor;
import static com.buschmais.jqassistant.core.model.test.matcher.descriptor.TypeDescriptorMatcher.typeDescriptor;
import static com.buschmais.jqassistant.core.model.test.matcher.rule.ConstraintMatcher.constraint;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.Assert.assertThat;

/**
 * Tests for the dependency concepts and result.
 */
public class DependencyIT extends AbstractAnalysisIT {

    /**
     * Verifies the concept "dependency:TypeBody".
     *
     * @throws java.io.IOException If the test fails.
     * @throws AnalyzerException   If the test fails.
     */
    @Test
    public void typeBodies() throws IOException, AnalyzerException {
        scanClasses(TypeBody.class);
        applyConcept("dependency:TypeBody");
        TestResult testResult = query("MATCH (t1:TYPE)-[:DEPENDS_ON]->(t2:TYPE) RETURN t2");
        // field
        assertThat(testResult.getColumn("t2"), allOf(hasItem(typeDescriptor(List.class)), hasItem(typeDescriptor(String.class)), hasItem(typeDescriptor(FieldAnnotation.class))));
        // method
        assertThat(testResult.getColumn("t2"), allOf(hasItem(typeDescriptor(Iterator.class)), hasItem(typeDescriptor(Number.class)), hasItem(typeDescriptor(Integer.class)), hasItem(typeDescriptor(Exception.class)), hasItem(typeDescriptor(Double.class)), hasItem(typeDescriptor(Boolean.class)), hasItem(typeDescriptor(MethodAnnotation.class))));
    }

    /**
     * Verifies the concept "dependency:Type".
     *
     * @throws java.io.IOException If the test fails.
     * @throws AnalyzerException   If the test fails.
     */
    @Test
    public void types() throws IOException, AnalyzerException {
        scanClasses(DependentType.class);
        applyConcept("dependency:Type");
        TestResult testResult = query("MATCH (t1:TYPE)-[:DEPENDS_ON]->(t2:TYPE) RETURN t2");
        // field
        assertThat(testResult.getColumn("t2"), allOf(hasItem(typeDescriptor(SuperType.class)), hasItem(typeDescriptor(Comparable.class)), hasItem(typeDescriptor(Integer.class)), hasItem(typeDescriptor(TypeAnnotation.class))));
    }

    /**
     * Verifies the concept "dependency:Package".
     *
     * @throws java.io.IOException If the test fails.
     * @throws AnalyzerException   If the test fails.
     */
    @Test
    public void packages() throws IOException, AnalyzerException {
        scanClasses(A.class, B.class);
        applyConcept("dependency:Package");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("package", A.class.getPackage().getName());
        assertThat(query("MATCH (p1:PACKAGE)-[:DEPENDS_ON]->(p2:PACKAGE) WHERE p1.FQN={package} RETURN p2", parameters).getColumn("p2"), hasItem(packageDescriptor(B.class.getPackage())));
        parameters.put("package", B.class.getPackage().getName());
        assertThat(query("MATCH (p1:PACKAGE)-[:DEPENDS_ON]->(p2:PACKAGE) WHERE p1.FQN={package} RETURN p2", parameters).getColumn("p2"), hasItem(packageDescriptor(A.class.getPackage())));
    }

    /**
     * Verifies the concept "dependency:Artifact".
     *
     * @throws java.io.IOException If the test fails.
     * @throws AnalyzerException   If the test fails.
     */
    @Test
    public void artifacts() throws IOException, AnalyzerException {
        scanClasses("a", A.class);
        scanClasses("b", B.class);
        applyConcept("dependency:Artifact");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("artifact", "a");
        assertThat(query("MATCH (a1:ARTIFACT)-[:DEPENDS_ON]->(a2:ARTIFACT) WHERE a1.FQN={artifact} RETURN a2", parameters).getColumn("a2"), hasItem(artifactDescriptor("b")));
        parameters.put("artifact", "b");
        assertThat(query("MATCH (a1:ARTIFACT)-[:DEPENDS_ON]->(a2:ARTIFACT) WHERE a1.FQN={artifact} RETURN a2", parameters).getColumn("a2"), hasItem(artifactDescriptor("a")));
    }

    /**
     * Verifies the constraint "dependency:PackageCycles".
     *
     * @throws java.io.IOException If the test fails.
     * @throws AnalyzerException   If the test fails.
     */
    @Test
    public void packageCycles() throws IOException, AnalyzerException {
        scanClasses(A.class);
        scanClasses(B.class);
        validateConstraint("dependency:PackageCycles");
        List<Result<Constraint>> constraintViolations = reportWriter.getConstraintViolations();
        Matcher<Iterable<? super Result<Constraint>>> matcher = hasItem(result(constraint("dependency:PackageCycles")));
        assertThat(constraintViolations, matcher);
    }

    /**
     * Verifies the constraint "dependency:TypeCycles".
     *
     * @throws java.io.IOException If the test fails.
     * @throws AnalyzerException   If the test fails.
     */
    @Test
    public void typeCycles() throws IOException, AnalyzerException {
        scanClasses( A.class);
        scanClasses(B.class);
        validateConstraint("dependency:TypeCycles");
        List<Result<Constraint>> constraintViolations = reportWriter.getConstraintViolations();
        Matcher<Iterable<? super Result<Constraint>>> matcher = hasItem(result(constraint("dependency:TypeCycles")));
        assertThat(constraintViolations, matcher);
    }

    /**
     * Verifies the constraint "dependency:ArtifactCycles".
     *
     * @throws java.io.IOException If the test fails.
     * @throws AnalyzerException   If the test fails.
     */
    @Test
    public void artifactCycles() throws IOException, AnalyzerException {
        scanClasses("a", A.class);
        scanClasses("b", B.class);
        validateConstraint("dependency:ArtifactCycles");
        List<Result<Constraint>> constraintViolations = reportWriter.getConstraintViolations();
        Matcher<Iterable<? super Result<Constraint>>> matcher = hasItem(result(constraint("dependency:ArtifactCycles")));
        assertThat(constraintViolations, matcher);
    }}
