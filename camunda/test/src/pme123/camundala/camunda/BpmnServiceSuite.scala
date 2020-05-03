package pme123.camundala.camunda

import pme123.camundala.camunda.xml.XmlHelper
import pme123.camundala.camunda.xml.XmlHelper.XQualifier
import pme123.camundala.model._
import pme123.camundala.model.bpmn.bpmnRegister
import zio.test.Assertion._
import zio.test._
import zio.test.junit.JUnitRunnableSpec

import scala.xml._

object BpmnServiceSuite extends JUnitRunnableSpec {
import TestData._

  def spec: ZSpec[environment.TestEnvironment, Any] =
    suite("BpmnSuite")(
      testM("the BPMN Model is merged") {
        for {
          _ <- bpmnRegister.registerBpmn(bpmn)
          mergeResult <- bpmnService.mergeBpmn("TwitterDemoProcess.bpmn", XML.load(bpmnResource.reader()))
          userTasks = mergeResult.xmlElem \\ "userTask"
          serviceTasks = mergeResult.xmlElem \\ "serviceTask"
          sendTasks = mergeResult.xmlElem \\ "sendTask"
          startEvents = mergeResult.xmlElem \\ "startEvent"
          exclusiveGateways = mergeResult.xmlElem \\ "exclusiveGateway"
          userTaskProps = userTasks.head \\ "property"
          delegateExpression = XQualifier.camunda(XmlHelper.delegateExpression).extractFrom(serviceTasks.head)
          externalTask = XQualifier.camunda("topic").extractFrom(serviceTasks.last)
          serviceTaskProp = serviceTasks.head \\ "property"
          startEventProp = startEvents \\ "property"
          exclusiveGatewayProp = exclusiveGateways.head \\ "property"
        } yield {
          assert(mergeResult.warnings.value.length)(equalTo(2) ?? "warnings") &&
            assert(mergeResult.warnings.value.head.msg)(equalTo("You have 2 ExclusiveGateway in the XML-Model, but you have 1 in Scala")) &&
            assert(mergeResult.warnings.value(1).msg)(equalTo("There is NOT a ExclusiveGateway with id 'gateway_join' in Scala.")) &&
            assert(userTasks.size)(equalTo(1) ?? "userTasks") &&
            assert(userTaskProps.length)(equalTo(3) ?? "userTaskProps") &&
            assert(serviceTasks.length)(equalTo(3) ?? "serviceTasks") &&
            assert(sendTasks.length)(equalTo(1) ?? "sendTasks") &&
            assert(serviceTaskProp.length)(equalTo(1) ?? "serviceTaskProp") &&
            assert(startEvents.length)(equalTo(2) ?? "startEvents") &&
            assert(startEventProp.length)(equalTo(1) ?? "startEventProp") &&
            assert(exclusiveGateways.length)(equalTo(2) ?? "exclusiveGateways") &&
            assert(exclusiveGatewayProp.length)(equalTo(1) ?? "exclusiveGatewayProp") &&
            assert(delegateExpression)(equalTo(Some("#{emailAdapter}"))) &&
            assert(externalTask)(equalTo(Some("myTopic")))

        }
      }
    ).provideCustomLayer(((bpmnRegister.live >>> bpmnService.live) ++ bpmnRegister.live).mapError(TestFailure.fail))
}
