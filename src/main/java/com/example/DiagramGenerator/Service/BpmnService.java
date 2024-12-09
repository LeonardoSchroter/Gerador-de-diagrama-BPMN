package com.example.DiagramGenerator.Service;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnEdge;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnPlane;
import org.camunda.bpm.model.bpmn.instance.bpmndi.BpmnShape;
import org.camunda.bpm.model.bpmn.instance.dc.Bounds;
import org.camunda.bpm.model.bpmn.instance.di.Waypoint;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BpmnService {


    public BpmnModelInstance generateBpmn(List<String> taskNames) {
        // Criar o modelo BPMN vazio
        BpmnModelInstance modelInstance = Bpmn.createEmptyModel();

        // Configurar as definições do modelo
        Definitions definitions = modelInstance.newInstance(Definitions.class);
        definitions.setId("Definitions_" + UUID.randomUUID());
        definitions.setTargetNamespace("http://camunda.org/examples");
        modelInstance.setDefinitions(definitions);

        // Criar o processo principal
        Process process = modelInstance.newInstance(Process.class);
        process.setId("Process_" + UUID.randomUUID());
        definitions.addChildElement(process);

        // Criar evento inicial
        StartEvent startEvent = createElement(modelInstance, process, "startEvent", StartEvent.class);
        addDiagramElement(modelInstance, startEvent, 100, 150, 30, 30);

        FlowNode lastNode = startEvent;
        List<Task> tasks = new ArrayList<>();

        // Criar todas as tarefas
        int x = 200; // Posição inicial em X para tarefas
        for (String taskName : taskNames) {
            Task task = createElement(modelInstance, process, "task_" + UUID.randomUUID(), Task.class);
            task.setName(taskName);
            tasks.add(task);
            addDiagramElement(modelInstance, task, x, 150, 80, 50);
            x += 150;
        }

        // Criar evento final
        EndEvent endEvent = createElement(modelInstance, process, "endEvent", EndEvent.class);
        addDiagramElement(modelInstance, endEvent, x, 150, 30, 30);

        // Conectar as tarefas com SequenceFlow
        FlowNode previousNode = startEvent;
        for (Task task : tasks) {
            createSequenceFlow(modelInstance, previousNode, task);
            previousNode = task;
        }
        createSequenceFlow(modelInstance, previousNode, endEvent);

        Bpmn.validateModel(modelInstance);

        return modelInstance;
    }

    private <T extends FlowElement> T createElement(BpmnModelInstance modelInstance, Process process, String id, Class<T> elementClass) {
        T element = modelInstance.newInstance(elementClass);
        element.setId(id);
        process.addChildElement(element); // Aqui adicionamos diretamente no processo
        return element;
    }

    public SequenceFlow createSequenceFlow(BpmnModelInstance modelInstance, FlowNode from, FlowNode to) {
        // Criar o SequenceFlow
        SequenceFlow sequenceFlow = modelInstance.newInstance(SequenceFlow.class);
        sequenceFlow.setId(from.getId() + "_" + to.getId() + "_flow");
        sequenceFlow.setSource(from);
        sequenceFlow.setTarget(to);

        // Associar o SequenceFlow ao processo principal
        Process process = (Process) from.getParentElement();
        process.addChildElement(sequenceFlow);

        // Conectar o fluxo aos nós
        from.getOutgoing().add(sequenceFlow);
        to.getIncoming().add(sequenceFlow);

        // Adicionar ao diagrama visual (BpmnEdge)
        addDiagramElement(modelInstance, sequenceFlow, 0, 0, 0, 0);  // O posicionamento será calculado automaticamente

        return sequenceFlow;
    }

    private void addDiagramElement(BpmnModelInstance modelInstance, FlowElement element, double x, double y, double width, double height) {
        BpmnDiagram diagram = modelInstance.getModelElementsByType(BpmnDiagram.class).stream().findFirst()
                .orElseGet(() -> {
                    BpmnDiagram newDiagram = modelInstance.newInstance(BpmnDiagram.class);
                    modelInstance.getDefinitions().addChildElement(newDiagram);
                    return newDiagram;
                });

        // Criar ou obter o BpmnPlane
        BpmnPlane plane = modelInstance.getModelElementsByType(BpmnPlane.class).stream().findFirst()
                .orElseGet(() -> {
                    BpmnPlane newPlane = modelInstance.newInstance(BpmnPlane.class);
                    newPlane.setBpmnElement(modelInstance.getModelElementsByType(Process.class).iterator().next());
                    diagram.addChildElement(newPlane);
                    return newPlane;
                });

        // Para FlowNode, cria e adiciona o BpmnShape
        if (element instanceof FlowNode) {
            BpmnShape shape = modelInstance.newInstance(BpmnShape.class);
            shape.setBpmnElement((FlowNode) element);

            Bounds bounds = modelInstance.newInstance(Bounds.class);
            bounds.setX(x);
            bounds.setY(y);
            bounds.setWidth(width);
            bounds.setHeight(height);

            shape.setBounds(bounds);
            plane.addChildElement(shape); // Aqui adicionamos diretamente ao BpmnPlane
        } else if (element instanceof SequenceFlow) {
            // Para SequenceFlow, cria e adiciona o BpmnEdge
            SequenceFlow flow = (SequenceFlow) element;
            BpmnEdge edge = modelInstance.newInstance(BpmnEdge.class);
            edge.setBpmnElement(flow);

            FlowNode sourceNode = flow.getSource();
            FlowNode targetNode = flow.getTarget();

            BpmnShape sourceShape = getShape(modelInstance, sourceNode);
            BpmnShape targetShape = getShape(modelInstance, targetNode);

            Waypoint waypoint1 = modelInstance.newInstance(Waypoint.class);
            waypoint1.setX(sourceShape.getBounds().getX() + sourceShape.getBounds().getWidth() / 2);
            waypoint1.setY(sourceShape.getBounds().getY() + sourceShape.getBounds().getHeight() / 2);

            Waypoint waypoint2 = modelInstance.newInstance(Waypoint.class);
            waypoint2.setX(targetShape.getBounds().getX() + targetShape.getBounds().getWidth() / 2);
            waypoint2.setY(targetShape.getBounds().getY() + targetShape.getBounds().getHeight() / 2);

            edge.addChildElement(waypoint1);
            edge.addChildElement(waypoint2);
            plane.addChildElement(edge);  // Aqui adicionamos diretamente ao BpmnPlane
        }
    }

    private BpmnShape getShape(BpmnModelInstance modelInstance, FlowNode node) {
        return modelInstance.getModelElementsByType(BpmnShape.class).stream()
                .filter(shape -> shape.getBpmnElement().equals(node))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Shape not found for node: " + node.getId()));
    }
}
