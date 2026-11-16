package com.visual.backend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/** Regla de dependencia entre capas y modulos (diagrama de paquetes). */
class ArquitecturaTest {

    private static final String RAIZ = "com.visual.backend";
    private static final List<String> MODULOS = List.of(
            "accesos", "activos", "comercial", "finanzas", "gerencial", "logistica", "respaldos", "subcontratacion");

    /*
     * Acoplamiento aceptado por compartir la base de datos: estos modulos usan
     * entidades JPA de otros en su persistencia (usuarios de auditoria,
     * proyectos, eventos y activos). Los modulos nuevos no deben sumarse aqui.
     */
    private static final String PERSISTENCIA_ACCESOS = paquete("accesos", "infraestructura.persistencia");
    private static final String PERSISTENCIA_COMERCIAL = paquete("comercial", "infraestructura.persistencia");
    private static final String PERSISTENCIA_ACTIVOS = paquete("activos", "infraestructura.persistencia");
    private static final Map<String, List<String>> ACOPLAMIENTO_ACEPTADO = Map.of(
            "activos", List.of(PERSISTENCIA_ACCESOS),
            "comercial", List.of(PERSISTENCIA_ACCESOS),
            "finanzas", List.of(PERSISTENCIA_ACCESOS, PERSISTENCIA_COMERCIAL, PERSISTENCIA_ACTIVOS),
            "logistica", List.of(PERSISTENCIA_ACCESOS, PERSISTENCIA_COMERCIAL));

    private static JavaClasses clases;

    @BeforeAll
    static void importar() {
        clases = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(RAIZ);
    }

    @Test
    void elDominioNoDependeDeOtrasCapasNiDeFrameworks() {
        noClasses().that().resideInAPackage("..dominio..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..aplicacion..", "..infraestructura..", "org.springframework..", "jakarta.persistence..")
                .check(clases);
    }

    @Test
    void laAplicacionNoDependeDeLaInfraestructura() {
        noClasses().that().resideInAPackage("..aplicacion..")
                .should().dependOnClassesThat().resideInAPackage("..infraestructura..")
                .check(clases);
    }

    @Test
    void elNucleoCompartidoNoDependeDeLosModulos() {
        noClasses().that().resideInAPackage(RAIZ + ".compartido..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        MODULOS.stream().map(modulo -> RAIZ + "." + modulo + "..").toArray(String[]::new))
                .check(clases);
    }

    @Test
    void ningunModuloUsaLaInfraestructuraDeOtroSalvoElAcoplamientoAceptado() {
        for (String modulo : MODULOS) {
            noClasses().that().resideInAPackage(RAIZ + "." + modulo + "..")
                    .should().dependOnClassesThat(infraestructuraAjena(modulo))
                    .check(clases);
        }
    }

    private static DescribedPredicate<JavaClass> infraestructuraAjena(String modulo) {
        List<String> aceptados = ACOPLAMIENTO_ACEPTADO.getOrDefault(modulo, List.of());
        return DescribedPredicate.describe("infraestructura de otro modulo", clase -> {
            String paquete = clase.getPackageName();
            boolean ajena = MODULOS.stream()
                    .filter(otro -> !otro.equals(modulo))
                    .anyMatch(otro -> perteneceA(paquete, paquete(otro, "infraestructura")));
            return ajena && aceptados.stream().noneMatch(aceptado -> perteneceA(paquete, aceptado));
        });
    }

    private static boolean perteneceA(String paquete, String base) {
        return paquete.equals(base) || paquete.startsWith(base + ".");
    }

    private static String paquete(String modulo, String capa) {
        return RAIZ + "." + modulo + "." + capa;
    }
}
