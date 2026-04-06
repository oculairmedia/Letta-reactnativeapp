import { formatRelativeTime } from "../formatters"

describe("formatRelativeTime", () => {
  beforeEach(() => {
    jest.useFakeTimers()
    // Set current time to a fixed date: 2024-06-15T12:00:00.000Z
    jest.setSystemTime(new Date("2024-06-15T12:00:00.000Z"))
  })

  afterEach(() => {
    jest.useRealTimers()
  })

  it('should return empty string for null input', () => {
    expect(formatRelativeTime(null)).toBe("")
  })

  it('should return empty string for undefined input', () => {
    expect(formatRelativeTime(undefined)).toBe("")
  })

  it('should return "now" for times less than a minute ago', () => {
    const thirtySecondsAgo = new Date("2024-06-15T11:59:30.000Z").toISOString()
    expect(formatRelativeTime(thirtySecondsAgo)).toBe("now")
  })

  it('should return minutes ago for times less than an hour ago', () => {
    const fiveMinutesAgo = new Date("2024-06-15T11:55:00.000Z").toISOString()
    expect(formatRelativeTime(fiveMinutesAgo)).toBe("5m ago")

    const thirtyMinutesAgo = new Date("2024-06-15T11:30:00.000Z").toISOString()
    expect(formatRelativeTime(thirtyMinutesAgo)).toBe("30m ago")
  })

  it('should return hours ago for times less than a day ago', () => {
    const twoHoursAgo = new Date("2024-06-15T10:00:00.000Z").toISOString()
    expect(formatRelativeTime(twoHoursAgo)).toBe("2h ago")

    const twentyThreeHoursAgo = new Date("2024-06-14T13:00:00.000Z").toISOString()
    expect(formatRelativeTime(twentyThreeHoursAgo)).toBe("23h ago")
  })

  it('should return days ago for times less than a week ago', () => {
    const oneDayAgo = new Date("2024-06-14T12:00:00.000Z").toISOString()
    expect(formatRelativeTime(oneDayAgo)).toBe("1d ago")

    const sixDaysAgo = new Date("2024-06-09T12:00:00.000Z").toISOString()
    expect(formatRelativeTime(sixDaysAgo)).toBe("6d ago")
  })

  it('should return formatted date for times more than a week ago', () => {
    const twoWeeksAgo = new Date("2024-06-01T12:00:00.000Z").toISOString()
    // The exact format depends on locale, but it should be a short date
    const result = formatRelativeTime(twoWeeksAgo)
    expect(result).toMatch(/Jun\s+1/)

    const monthAgo = new Date("2024-05-15T12:00:00.000Z").toISOString()
    const monthResult = formatRelativeTime(monthAgo)
    expect(monthResult).toMatch(/May\s+15/)
  })

  it('should handle edge case at exactly 1 minute', () => {
    const oneMinuteAgo = new Date("2024-06-15T11:59:00.000Z").toISOString()
    expect(formatRelativeTime(oneMinuteAgo)).toBe("1m ago")
  })

  it('should handle edge case at exactly 1 hour', () => {
    const oneHourAgo = new Date("2024-06-15T11:00:00.000Z").toISOString()
    expect(formatRelativeTime(oneHourAgo)).toBe("1h ago")
  })

  it('should handle edge case at exactly 1 day', () => {
    const oneDayAgo = new Date("2024-06-14T12:00:00.000Z").toISOString()
    expect(formatRelativeTime(oneDayAgo)).toBe("1d ago")
  })
})
