package de.andrena.tools.nopackagecycles;

import static de.andrena.tools.nopackagecycles.CollectionOutput.joinArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import jdepend.framework.JavaClass;
import jdepend.framework.JavaPackage;

import org.junit.Before;
import org.junit.Test;

import de.andrena.tools.nopackagecycles.CollectionOutput.StringProvider;
import edu.emory.mathcs.backport.java.util.Arrays;

public class PackageCycleOutputTest {

	private static final String PACKAGE1_CLASS_NAME1 = "Package1Class1";
	private static final String PACKAGE1_CLASS_NAME2 = "Package1Class2";
	private static final String PACKAGE2_CLASS_NAME = "Package2Class";
	private static final String PACKAGE1_NAME = "sample.package1";
	private static final String PACKAGE2_NAME = "sample.package2";
	private List<JavaPackage> packages;
	private JavaPackage package1;
	private JavaPackage package2;

	@Before
	public void setUp() {
		packages = new ArrayList<JavaPackage>();
		initDefaultPackages();
		package1.dependsUpon(package2);
		package2.dependsUpon(package1);
	}

	@Test
	public void outputFor_TwoPackagesWithCycle() throws Exception {
		assertOutput(getPackageOutput(package1, package2) + getPackageOutput(package2, package1));
	}

	@Test
	public void outputFor_TwoPackagesWithCycleAndClasses() throws Exception {
		JavaClass package1Class = createClassInPackage(PACKAGE1_CLASS_NAME1, package1);
		package1Class.addImportedPackage(package2);
		JavaClass package2Class = createClassInPackage(PACKAGE2_CLASS_NAME, package2);
		package2Class.addImportedPackage(package1);
		assertOutput(getPackageOutputWithClasses(package1, package2, PACKAGE2_CLASS_NAME)
				+ getPackageOutputWithClasses(package2, package1, PACKAGE1_CLASS_NAME1));
	}

	@Test
	public void outputFor_TwoPackagesWithCycle_AndOnePackageWithoutCycle() throws Exception {
		String packageWithoutCycleName = "sample.package.without.cycle";
		createPackage(packageWithoutCycleName);
		assertOutput(getPackageOutput(package1, package2) + getPackageOutput(package2, package1));
	}

	@Test
	public void outputFor_ThreePackagesWithCycle() throws Exception {
		String package3Name = "sample.package3";
		JavaPackage package3 = createPackage(package3Name);
		package1.dependsUpon(package3);
		package3.dependsUpon(package1);
		assertOutput(getPackageOutput(package1, package2, package3) + getPackageOutput(package2, package1, package3)
				+ getPackageOutput(package3, package1, package2));
	}

	@Test
	public void outputFor_PackageWithCycleAndMultipleClasses() throws Exception {
		JavaClass package1Class1 = createClassInPackage(PACKAGE1_CLASS_NAME1, package1);
		package1Class1.addImportedPackage(package2);
		JavaClass package1Class2 = createClassInPackage(PACKAGE1_CLASS_NAME2, package1);
		package1Class2.addImportedPackage(package2);
		assertOutput(getPackageOutput(package1, package2)
				+ getPackageOutputWithClasses(package2, package1, PACKAGE1_CLASS_NAME1, PACKAGE1_CLASS_NAME2));
	}

	@Test
	public void outputFor_PackageWithCycleAndClassWithoutCycle() throws Exception {
		JavaClass package1Class1 = createClassInPackage(PACKAGE1_CLASS_NAME1, package1);
		package1Class1.addImportedPackage(package2);
		createClassInPackage(PACKAGE1_CLASS_NAME2, package1);
		assertOutput(getPackageOutput(package1, package2)
				+ getPackageOutputWithClasses(package2, package1, PACKAGE1_CLASS_NAME1));
	}

	@Test
	public void outputFor_MultiplePackageCycles_IsOrderedByName() {
		JavaPackage otherPackage1 = createPackage("other.package1");
		JavaPackage otherPackage2 = createPackage("other.package2");
		otherPackage1.dependsUpon(otherPackage2);
		otherPackage2.dependsUpon(otherPackage1);
		assertOutput(getPackageOutput(otherPackage1, otherPackage2) + getPackageOutput(otherPackage2, otherPackage1)
				+ getPackageOutput(package1, package2) + getPackageOutput(package2, package1));
	}

	@Test
	public void outputFor_MultiplePackageCycles_IsOrderedByCycle() {
		initDefaultPackages();
		JavaPackage otherPackage1 = createPackage("other.package1");
		JavaPackage otherPackage2 = createPackage("other.package2");
		package1.dependsUpon(otherPackage1);
		otherPackage1.dependsUpon(package1);
		package2.dependsUpon(otherPackage2);
		otherPackage2.dependsUpon(package2);
		assertOutput(getPackageOutput(otherPackage1, package1) + getPackageOutput(package1, otherPackage1)
				+ getPackageOutput(otherPackage2, package2) + getPackageOutput(package2, otherPackage2));
	}

	private JavaPackage createPackage(String package1Name) {
		JavaPackage newPackage = new JavaPackage(package1Name);
		packages.add(newPackage);
		return newPackage;
	}

	private void initDefaultPackages() {
		packages.clear();
		package1 = createPackage(PACKAGE1_NAME);
		package2 = createPackage(PACKAGE2_NAME);
	}

	private JavaClass createClassInPackage(String className, JavaPackage classPackage) {
		JavaClass package1Class = new JavaClass(classPackage.getName() + "." + className);
		classPackage.addClass(package1Class);
		return package1Class;
	}

	private String getPackageOutputWithClasses(JavaPackage javaPackage, JavaPackage dependencyPackage,
			String... classNames) {
		String joinedClassNames = joinArray(classNames, new StringProvider<String>() {
			public String provide(String value) {
				return value;
			}
		});
		return getPackageOutput(javaPackage.getName())
				+ getDependencyPackageOutput(dependencyPackage.getName(), joinedClassNames);
	}

	private String getPackageOutput(JavaPackage javaPackage, JavaPackage... dependencyPackages) {
		@SuppressWarnings("unchecked")
		List<JavaPackage> dependencyPackageList = Arrays.asList(dependencyPackages);
		String dependencyOutput = CollectionOutput.joinCollection(dependencyPackageList,
				new StringProvider<JavaPackage>() {
					public String provide(JavaPackage dependencyPackage) {
						return getDependencyPackageOutput(dependencyPackage.getName(), "");
					}
				});
		return getPackageOutput(javaPackage.getName()) + dependencyOutput;
	}

	private void assertOutput(String output) {
		assertThat(new PackageCycleOutput(packages).getOutput(), is(output));
	}

	private String getPackageOutput(String packageName) {
		return "\n" + packageName + " has cyclic dependency to: ";
	}

	private String getDependencyPackageOutput(String dependencyPackageNames, String classNames) {
		return dependencyPackageNames + " (" + classNames + ")";
	}
}