package spinal.core

import java.util

import scala.StringBuilder
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, StringBuilder}


class ComponentBuilder(val component: Component) {
  val parts = ArrayBuffer[(StringBuilder, Boolean)]()

  def newPart(mustMatch: Boolean): StringBuilder = {
    val builder = new mutable.StringBuilder
    parts += (builder -> mustMatch)
    builder
  }

  def result: String = {
    val ret = new mutable.StringBuilder
    parts.foreach(ret ++= _._1)
    ret.result()
  }

  var hash: Integer = null

  override def hashCode(): Int = {
    if (hash == null) {
      hash = parts.filter(_._2).foldLeft(0)(_ + _._1.result().hashCode())
    }
    hash
  }

  override def equals(obj: scala.Any): Boolean = {
    if (this.hashCode() != obj.hashCode()) return false //Colision into hashmap implementation don't check it XD
    obj match {
      case cb: ComponentBuilder => {
        for ((a, b) <- (parts, cb.parts).zipped) {
          if (a._2 || b._2) {
            if (a._1.result() != b._1.result()) {
              return false
            }
          }
        }
        return true;
      }
      case _ => return ???
    }
  }
}


class ConditionalTree

class WhenTree(val cond: Node,val context : WhenContext) extends ConditionalTree{
  var whenTrue: AssignementLevel = new AssignementLevel
  var whenFalse: AssignementLevel = new AssignementLevel
}


case class SwitchTreeCase(const : Node,doThat : AssignementLevel)
case class SwitchTreeDefault(doThat : AssignementLevel)


class SwitchTree(key: Node) extends ConditionalTree {
  val cases = ArrayBuffer[SwitchTreeCase]()
  var default : SwitchTreeDefault = null
}

class AdditiveList[T]{
  var headNode : AdditiveListNode[T] = null

  def isEmpty = headNode == null

  def iterator = new AdditiveListIterator(this)
  def head = headNode.elem
  def onEachNode(doThat : (AdditiveListNode[T]) => Unit): Unit ={
    var ptr = headNode
    while(ptr != null){
      doThat(ptr)
      ptr = ptr.next
    }
  }
  def onEach(doThat : (T) => Unit): Unit ={
    var ptr = headNode
    while(ptr != null){
      doThat(ptr.elem)
      ptr = ptr.next
    }
  }
}

class AdditiveListIterator[T](list : AdditiveList[T]){
  var position : AdditiveListNode[T] = list.headNode

  def += (that : T) : Unit = {
    if(position == null){
      position = AdditiveListNode(that)
      list.headNode = position
    }else{
      position = (position += that)
    }
  }

  def seek(that : AdditiveListNode[T]) : Unit = position = that

  def value = position.elem
}

case class AdditiveListNode[T](elem : T) {
  var next : AdditiveListNode[T] = null

  def +=(that : T) : AdditiveListNode[T] = {
    val ret = AdditiveListNode(that)
    ret.next = this.next
    next = ret
    ret
  }
}

case class AssignementTask(that : Node,by : Node)




class AssignementLevel {
  val content = new AdditiveList[Any]()
  val whenMap = mutable.HashMap[WhenContext,AdditiveListNode[Any]]()

  def isNotEmpty = !content.isEmpty
  def isOnlyAWhen = isNotEmpty && content.headNode.elem.isInstanceOf[WhenTree] && content.headNode.next == null

  def walkNodes(thats : Seq[AssignementTask]) : Unit = {
    def getElements(that : Node): Iterator[Node] = if (that.isInstanceOf[MultipleAssignmentNode])
      that.getInputs else Iterator(that)

    thats.foreach(_.by match {
      case by : WhenNode => by.w.algoId = 0
      case by : Node => by.algoId = 0
    })
    thats.foreach(_.by match {
      case by : WhenNode => by.w.algoId += 1
      case by : Node => by.algoId += 1
    })

  }

  def walkWhenTree(root: Node, that: Node,ptr_ : AdditiveListIterator[Any] = content.iterator): Unit = {
    val ptr = ptr_
    def getElements: Iterator[Node] = if (that.isInstanceOf[MultipleAssignmentNode])
        that.getInputs else Iterator(that)


    for (node <- getElements) {
      node match {
        case whenNode: WhenNode => {
          def getWhenTree(): WhenTree = {
            whenMap.get(whenNode.w) match {
              case Some(listNode) => {
                ptr.seek(listNode)
                ptr.value.asInstanceOf[WhenTree]
              }
              case None => {
                val whenTree = new WhenTree(whenNode.cond,whenNode.w)
                ptr += whenTree
                whenMap.put(whenNode.w,ptr.position)
                whenTree
              }
            }
          }
          if (!whenNode.whenTrue.isInstanceOf[NoneNode]) {
            getWhenTree().whenTrue.walkWhenTree(root, whenNode.whenTrue)
          }
          if (!whenNode.whenFalse.isInstanceOf[NoneNode]) {
            getWhenTree().whenFalse.walkWhenTree(root, whenNode.whenFalse)
          }
        }
        case reg: Reg =>
        case _ => ptr += new AssignementTask(root, node)
      }
    }
  }

//  def caseify() : Unit = {
//    var ptr = content.next
//    while(ptr != null){
//      ptr.elem match {
//        case whenTree: WhenTree => {
//          whenTree.cond match {
//            case cond : Operator.Enum.Equal => {
//              (cond.left,cond.right) match {
//                case (c : SpinalEnumCraft[_],l : EnumLiteral[_]) => {
//                  println("yolo")
//                }
//                case _ =>
//              }
//            }
//            case _ =>
//          }
//        }
//        case _ =>
//      }
//      ptr = ptr.next
//    }
//  }
//
//  def casifyReq() : Unit = {
//    caseify()
//    var ptr = content.next
//    while(ptr != null){
//      ptr.elem match {
//        case whenTree: WhenTree => {
//          whenTree.cond match {
//            case cond : Operator.Enum.Equal => {
//              (cond.left,cond.right) match {
//                case (c : SpinalEnumCraft[_],l : EnumLiteral[_]) => {
//                  println("yolo")
//                }
//                case _ =>
//              }
//            }
//            case _ =>
//          }
//        }
//        case _ =>
//      }
//      ptr = ptr.next
//    }
//  }
}


trait MemBitsMaskKind
object MULTIPLE_RAM extends MemBitsMaskKind
object SINGLE_RAM extends MemBitsMaskKind


trait VhdlVerilogBase {
  def isReferenceable(node: Node) = node.isInstanceOf[Nameable]

  def getSensitivity(nodes: Iterable[Node], includeNodes: Boolean): mutable.Set[Node] = {
    val sensitivity = mutable.Set[Node]()

    if (includeNodes)
      nodes.foreach(walk(_))
    else
      nodes.foreach(_.onEachInput(walk(_)))

    def walk(node: Node): Unit = {
      if (isReferenceable(node))
        sensitivity += node
      else
        node.onEachInput(walk(_))
    }

    sensitivity
  }


  class Process(val order: Int) {
    var sensitivity: mutable.Set[Node] = null
    val nodes = ArrayBuffer[Node]()
    val whens = ArrayBuffer[ConditionalContext]()
    var hasMultipleAssignment = false

    def genSensitivity: Unit = sensitivity = getSensitivity(nodes, false)


    def needProcessDef: Boolean = {
      if (!whens.isEmpty || nodes.size > 1) return true
      if (hasMultipleAssignment) {
//        val ma: MultipleAssignmentNode = nodes(0).getInput(0).asInstanceOf[MultipleAssignmentNode]
//        val assignedBits = new AssignedBits(nodes(0).getWidth)
//        ma.onEachInput(_ match {
//          case assign: AssignementNode => {
//            val scope = assign.getScopeBits
//            if (!AssignedBits.intersect(scope, assignedBits).isEmpty) return true
//            assignedBits.add(scope)
//          }
//          case _ => return true
//        })
        return true; //Symplified because of Verilog backend
      }
      return false
    }
  }

  def getAsyncProcesses(component: Component) : Seq[Process] = {
    var processCounter = 0


    val processSet = mutable.Set[Process]()
    val whenToProcess = mutable.Map[ConditionalContext, Process]()

    def move(to: Process, from: Process): Unit = {
      to.nodes ++= from.nodes
      to.whens ++= from.whens
      to.hasMultipleAssignment |= from.hasMultipleAssignment
      from.whens.foreach(whenToProcess(_) = to)
      processSet.remove(from)
    }

    val asyncSignals = component.nodes.filter(_ match {
      case signal: BaseType => (!signal.isDelay) && (!((signal.isIo && signal.isInput) || component.kindsOutputsBindings.contains(signal)))
      case _ => false
    })

    for (signal <- asyncSignals) {
      var process: Process = null
      var hasMultipleAssignment = false
      walk(signal.getInput(0))
      def walk(that: Node): Unit = {
        that match {
          case wn: WhenNode => {
            if (whenToProcess.contains(wn.w)) {
              val otherProcess = whenToProcess.get(wn.w).get
              if (process == null) {
                process = otherProcess
                otherProcess.nodes += signal
              } else if (process != otherProcess) {
                move(otherProcess, process)
                process = otherProcess
              }
            } else {
              if (process == null) {
                process = new Process(processCounter);
                processCounter += 1
                process.nodes += signal
                processSet += process
              }
              process.whens += wn.w
              whenToProcess += (wn.w -> process)
            }

            walk(wn.whenTrue)
            walk(wn.whenFalse)
          }
          case switchNode: SwitchNode => {
            if (whenToProcess.contains(switchNode.context)) {
              val otherProcess = whenToProcess.get(switchNode.context).get
              if (process == null) {
                process = otherProcess
                otherProcess.nodes += signal
              } else if (process != otherProcess) {
                move(otherProcess, process)
                process = otherProcess
              }
            } else {
              if (process == null) {
                process = new Process(processCounter);
                processCounter += 1
                process.nodes += signal
                processSet += process
              }
              process.whens += switchNode.context
              whenToProcess += (switchNode.context -> process)
            }

            switchNode.cases.foreach(n => walk(n.asInstanceOf[CaseNode].assignement))
          }
          case man: MultipleAssignmentNode => {
            man.onEachInput(walk(_))
            hasMultipleAssignment = true
          }
          case that => {
            if (process == null) {
              process = new Process(processCounter);
              processCounter += 1
              process.nodes += signal
              processSet += process
            }
          }
        }
      }

      process.hasMultipleAssignment |= hasMultipleAssignment
    }

    val processList = processSet.toList.sortWith(_.order < _.order)
    processList
  }

}
