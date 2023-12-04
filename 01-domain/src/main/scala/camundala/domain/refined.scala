package camundala.domain

import io.github.iltotore.iron.constraint.all.*

// Json
type InOutJson = DescribedAs[
  Match["""(?(DEFINE)
  # Note that everything is atomic, JSON does not need backtracking if it's valid
  # and this prevents catastrophic backtracking
  (?<json>(?>\s*(?&object)\s*|\s*(?&array)\s*))
  (?<object>(?>\{\s*(?>(?&pair)(?>\s*,\s*(?&pair))*)?\s*\}))
  (?<pair>(?>(?&STRING)\s*:\s*(?&value)))
  (?<array>(?>\[\s*(?>(?&value)(?>\s*,\s*(?&value))*)?\s*\]))
  (?<value>(?>true|false|null|(?&STRING)|(?&NUMBER)|(?&object)|(?&array)))
  (?<STRING>(?>"(?>\\(?>["\\\/bfnrt]|u[a-fA-F0-9]{4})|[^"\\\0-\x1F\x7F]+)*"))
  (?<NUMBER>(?>-?(?>0|[1-9][0-9]*)(?>\.[0-9]+)?(?>[eE][+-]?[0-9]+)?))
  )
  \A(?&json)\z"""],
  "Valid Json"
]
val inOutJsonDescr = "Valid Json"

// Date Time
type Iso8601Day = DescribedAs[
  Match["""^([0-9]{4})-?(1[0-2]|0[1-9])-?(3[01]|0[1-9]|[12][0-9])$"""],
  "Day in ISO Format, like `yyyy-MM-dd` for example `2003-12-23`"
]
val iso8601DayDescr = "Day in ISO Format, like `yyyy-MM-dd` for example `2003-12-23`"

type Iso8601DateTime = DescribedAs[
  Match["""^([\+-]?\d{4}(?!\d{2}\b))((-?)((0[1-9]|1[0-2])(\3([12]\d|0[1-9]|3[01]))?|W([0-4]\d|5[0-2])(-?[1-7])?|(00[1-9]|0[1-9]\d|[12]\d{2}|3([0-5]\d|6[1-6])))([T\s]((([01]\d|2[0-3])((:?)[0-5]\d)?|24\:?00)([\.,]\d+(?!:))?)?(\17[0-5]\d([\.,]\d+)?)?([zZ]|([\+-])([01]\d|2[0-3]):?([0-5]\d)?)?)?)?$"""],
  "This must be a valid Iso8601 Date Time, like `2011-03-11T12:13:14Z`"
]
val iso8601DateTimeDescr =  "This must be a valid Iso8601 Date Time, like `2011-03-11T12:13:14Z`"

type Iso8601Duration = DescribedAs[
  Match["""^P(?!$)(?:(?<years>\d+(?:\.\d+)?)Y)?(?:(?<months>\d+(?:\.\d+)?)M)?(?:(?<weeks>\d+(?:\.\d+)?)W)?(?:(?<days>\d+(?:\.\d+)?)D)?(T(?=\d)(?:(?<hours>\d+(?:\.\d+)?)H)?(?:(?<minutes>\d+(?:\.\d+)?)M)?(?:(?<seconds>\d+(?:\.\d+)?)S)?)?$"""],
  "This must be a valid Iso8601 Duration Expression, like `PT3S`"
]
val iso8601DurationDescr = "This must be a valid Iso8601 Duration Expression, like `PT3S`"

type Iso8601Interval = DescribedAs[
  Match["""^(R\d*\/)P(\d+([\.,]\d+)?Y)?(\d+([\.,]\d+)?M)?(\d+([\.,]\d+)?W)?(\d+([\.,]\d+)?D)?((?!.$)T(\d+([\.,]\d+)?H)?(\d+([\.,]\d+)?M)?(\d+(\.\d+)?S)?)?$"""],
  "This must be a valid Iso8601 Repeating Interval Expression, like `R2/PT3S`"
]
val iso8601IntervalDescr = "This must be a valid Iso8601 Repeating Interval Expression, like `R2/PT3S`"

type CronExpr = DescribedAs[
  Match["""^\s*($|#|\w+\s*=|(\?|\*|(?:[0-5]?\d)(?:(?:-|\/|\,)(?:[0-5]?\d))?(?:,(?:[0-5]?\d)(?:(?:-|\/|\,)(?:[0-5]?\d))?)*)\s+(\?|\*|(?:[0-5]?\d)(?:(?:-|\/|\,)(?:[0-5]?\d))?(?:,(?:[0-5]?\d)(?:(?:-|\/|\,)(?:[0-5]?\d))?)*)\s+(\?|\*|(?:[01]?\d|2[0-3])(?:(?:-|\/|\,)(?:[01]?\d|2[0-3]))?(?:,(?:[01]?\d|2[0-3])(?:(?:-|\/|\,)(?:[01]?\d|2[0-3]))?)*)\s+(\?|\*|(?:0?[1-9]|[12]\d|3[01])(?:(?:-|\/|\,)(?:0?[1-9]|[12]\d|3[01]))?(?:,(?:0?[1-9]|[12]\d|3[01])(?:(?:-|\/|\,)(?:0?[1-9]|[12]\d|3[01]))?)*)\s+(\?|\*|(?:[1-9]|1[012])(?:(?:-|\/|\,)(?:[1-9]|1[012]))?(?:L|W)?(?:,(?:[1-9]|1[012])(?:(?:-|\/|\,)(?:[1-9]|1[012]))?(?:L|W)?)*|\?|\*|(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)(?:(?:-)(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))?(?:,(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)(?:(?:-)(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC))?)*)\s+(\?|\*|(?:[0-6])(?:(?:-|\/|\,|#)(?:[0-6]))?(?:L)?(?:,(?:[0-6])(?:(?:-|\/|\,|#)(?:[0-6]))?(?:L)?)*|\?|\*|(?:MON|TUE|WED|THU|FRI|SAT|SUN)(?:(?:-)(?:MON|TUE|WED|THU|FRI|SAT|SUN))?(?:,(?:MON|TUE|WED|THU|FRI|SAT|SUN)(?:(?:-)(?:MON|TUE|WED|THU|FRI|SAT|SUN))?)*)(|\s)+(\?|\*|(?:|\d{4})(?:(?:-|\/|\,)(?:|\d{4}))?(?:,(?:|\d{4})(?:(?:-|\/|\,)(?:|\d{4}))?)*))$"""],
  "This must be a valid Cron Expression, like `0 0 7,11,15 ? * MON-FRI`"
]
val cronExprDescr = "This must be a valid Cron Expression, like `0 0 7,11,15 ? * MON-FRI`"

// other
type Iban = DescribedAs[
  Match[
    """\b[A-Z]{2}[0-9]{2}(?:[ ]?[0-9]{4}){4}(?!(?:[ ]?[0-9]){3})(?:[ ]?[0-9]{1,2})?\b"""
  ],
  "This must be a valid IBAN"
]
val ibanDescr = "This must be a valid IBAN"

type Postcode = DescribedAs[
  Match["""(?i)^[a-z0-9][a-z0-9\- ]{0,10}[a-z0-9]$"""],
  "This must be a valid Postcode"
]
val postcodeDescr = "This must be a valid Postcode"
