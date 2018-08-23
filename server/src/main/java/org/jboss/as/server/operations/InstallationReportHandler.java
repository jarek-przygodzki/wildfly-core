/*
 * Copyright (C) 2015 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.server.operations;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.operations.global.GlobalInstallationReportHandler.SUMMARY_DEFINITION;

import java.nio.file.Path;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handler to produce a summary of the current server installation.
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2015 Red Hat, inc.
 */
public class InstallationReportHandler extends AbstractInstallationReporter {
    public static final SimpleOperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(
            OPERATION_NAME, ServerDescriptions.getResourceDescriptionResolver())
            .setRuntimeOnly()
            .setReadOnly()
            .withFlags(OperationEntry.Flag.HIDDEN) // can't be private because of how GlobalInstallationReportHandler calls it
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.READ_WHOLE_CONFIG)
            .setReplyType(ModelType.OBJECT)
            .setReplyParameters(SUMMARY_DEFINITION).build();

    public static InstallationReportHandler createOperation(final ServerEnvironment environment) {
        return new InstallationReportHandler(environment);
    }

    private final ServerEnvironment environment;

    private InstallationReportHandler(ServerEnvironment environment) {
        this.environment = environment;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode patchingInfo = new ModelNode();
        PathAddress patchingAddress = PathAddress.pathAddress(PathElement.pathElement(CORE_SERVICE, "patching"));
        OperationEntry opEntry = context.getRootResourceRegistration().getOperationEntry(patchingAddress, "show-history");
        if(opEntry != null) {
            context.addStep(patchingInfo, Util.createOperation("show-history", patchingAddress),
                opEntry.getOperationHandler(), OperationContext.Stage.RUNTIME);
        }
        final Path installationDir = environment.getHomeDir().toPath();
        context.addStep(new OperationStepHandler() {
             @Override
             public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                ModelNode result = context.getResult();
                result.get(SUMMARY_DEFINITION.getName()).set(createProductNode(context, new InstallationConfiguration(
                        environment, environment.getProductConfig(), patchingInfo, installationDir)));
             }
        }, OperationContext.Stage.RUNTIME);
    }
}