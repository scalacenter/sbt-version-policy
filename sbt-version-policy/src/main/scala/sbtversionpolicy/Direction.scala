package sbtversionpolicy

import dataclass.data

@data class Direction(
  backward: Boolean,
  forward: Boolean
)

object Direction {
  def none: Direction = Direction(false, false)
  def backward: Direction = Direction(true, false)
  def forward: Direction = Direction(false, true)
  def both: Direction = Direction(true, true)
}
