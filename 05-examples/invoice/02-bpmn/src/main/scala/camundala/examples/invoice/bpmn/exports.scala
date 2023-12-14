package camundala.examples.invoice.bpmn

import camundala.domain.*

enum InvoiceCategory:
  case `Travel Expenses`, Misc, `Software License Costs`

object InvoiceCategory:
  given ApiSchema[InvoiceCategory] = deriveEnumSchema
  given InOutCodec[InvoiceCategory] = deriveEnumCodec
end InvoiceCategory